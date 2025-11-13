package com.acme.obs.apm;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;


import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OtelApmClient extends AbstractApmClient {
  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;
  private final Meter meter;

  private final ConcurrentHashMap<String, LongCounter> counters = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, DoubleHistogram> histos = new ConcurrentHashMap<>();

  public OtelApmClient(ApmProperties props){
    super(props);

    Attributes resAttrs = Attributes.builder()
      .put(ResourceAttributes.SERVICE_NAME, defaulted(props.getServiceName(), "apm-demo"))
      .put(AttributeKey.stringKey("service.name"), defaulted(props.getServiceName(), "apm-demo"))
      .put(ResourceAttributes.SERVICE_VERSION, defaulted(props.getServiceVersion(), "0.0.0"))
      .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, defaulted(props.getEnvironment(), "local"))
      .build();

    Resource resource = Resource.getDefault().merge(Resource.create(resAttrs));

    OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
      .setEndpoint(defaulted(props.getEndpoint(), "http://localhost:4317"))
      .build();

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
      .setResource(resource)
      .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
          .setScheduleDelay(Duration.ofMillis(200))
          .build())
      .build();

    OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
      .setEndpoint(defaulted(props.getEndpoint(), "http://localhost:4317"))
      .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
      .setResource(resource)
      .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
          .setInterval(Duration.ofSeconds(10))
          .build())
      .build();

    this.openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .setMeterProvider(meterProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .buildAndRegisterGlobal();

    this.tracer = openTelemetry.getTracer("com.acme.obs.apm", "1.0.0");
    this.meter  = openTelemetry.getMeter("com.acme.obs.apm");
  }

  private static String defaulted(String v, String d){ return (v==null || v.isBlank())? d : v; }

  @Override
  protected ApmSpan doStartSpan(String name, Map<String,String> attrs){
    SpanKind kind = SpanKind.INTERNAL;
    if (attrs != null) {
      String k = attrs.getOrDefault("otel.kind", "");
      if ("server".equalsIgnoreCase(k)) kind = SpanKind.SERVER;
      else if ("client".equalsIgnoreCase(k)) kind = SpanKind.CLIENT;
    }
    var spanBuilder = tracer.spanBuilder(name).setSpanKind(kind);
    if (attrs != null) {
      AttributesBuilder ab = Attributes.builder();
      attrs.forEach((k,v) -> ab.put(AttributeKey.stringKey(k), String.valueOf(v)));
      var toSet = ab.build();
      toSet.forEach((attributeKey, o) -> spanBuilder.setAttribute((AttributeKey<Object>) attributeKey, o));
    }
    Span span = spanBuilder.startSpan();
    Scope scope = span.makeCurrent();

    return new ApmSpan() {
      @Override public ApmSpan setAttribute(String k, String v){ span.setAttribute(k, v); return this; }
      @Override public ApmSpan setAttribute(String k, long v){ span.setAttribute(k, v); return this; }
      @Override public ApmSpan setAttribute(String k, double v){ span.setAttribute(k, v); return this; }
      @Override public ApmSpan recordException(Throwable t){ span.recordException(t); span.setStatus(StatusCode.ERROR); return this; }
      @Override public void close(){ span.end(); scope.close(); }
    };
  }

  @Override public void addEvent(String name, Map<String, String> attributes) {
    Span.current().addEvent(name, mapToAttributes(attributes));
  }

  @Override public void recordException(Throwable t) {
    Span span = Span.current();
    if (span != null) { span.recordException(t); span.setStatus(StatusCode.ERROR); }
  }

  @Override public void setTransactionName(String name) {
    Span span = Span.current();
    if (span != null) span.updateName(name);
  }

  @Override public void setUser(String userId, Map<String, String> attrs) {
    Span span = Span.current();
    if (span != null) {
      span.setAttribute("user.id", userId);
      if (attrs != null) attrs.forEach((k,v) -> span.setAttribute("user."+k, v));
    }
  }

  @Override public void incrementCounter(String name, double amount, Map<String, String> tags) {
    LongCounter c = counters.computeIfAbsent(name, n -> meter.counterBuilder(n).build());
    c.add((long) amount, mapToAttributes(tags));
  }

  @Override public void recordHistogram(String name, double value, Map<String, String> tags) {
    DoubleHistogram h = histos.computeIfAbsent(name, n -> (DoubleHistogram) meter.histogramBuilder(n).ofLongs().build());
    h.record(value, mapToAttributes(tags));
  }

  @Override public void gauge(String name, double value, Map<String, String> tags) {
    // Emulado via histogram sample
    recordHistogram(name+".gauge", value, tags);
  }

  private Attributes mapToAttributes(Map<String,String> tags){
    AttributesBuilder b = Attributes.builder();
    if(tags!=null) tags.forEach((k,v) -> b.put(AttributeKey.stringKey(k), String.valueOf(v)));
    return b.build();
  }

  @Override public void injectContext(java.util.function.BiConsumer<String, String> headerSetter) {
    TextMapSetter<java.util.function.BiConsumer<String,String>> setter = (carrier, key, value) -> carrier.accept(key, value);
    openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), headerSetter, setter);
  }

  @Override public void extractContext(java.util.function.Function<String, String> headerGetter) {
    TextMapGetter<java.util.function.Function<String,String>> getter = new TextMapGetter<>() {
      @Override public Iterable<String> keys(java.util.function.Function<String, String> carrier) { return java.util.List.of("traceparent","tracestate"); }
      @Override public String get(java.util.function.Function<String, String> carrier, String key) {
        return carrier.apply(key);
      }
    };
    Context ctx = openTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), headerGetter, getter);
    ctx.makeCurrent();
  }
}
