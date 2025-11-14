package com.acme.obs.apm;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(10)
@Component
public class TracingAspect {
  private final ApmClient apm;
  public TracingAspect(ApmClient apm){ this.apm = apm; }

  @Around("@annotation(trace)")
  public Object around(ProceedingJoinPoint pjp, TraceSpan trace) throws Throwable {
    MethodSignature sig = (MethodSignature) pjp.getSignature();
    String defaultName = sig.getDeclaringType().getSimpleName()+"#"+sig.getMethod().getName();
    String spanName = trace.value().isEmpty()? defaultName : trace.value();

    SpanAttributeBuilder builder = SpanAttributeBuilder.create()
      .withLayer(trace.layer())
      .withComponentDetails(sig.getDeclaringType())
      .withMethod(sig.getMethod())
      .withDeclaredAttributes(trace.attributes());

    if(trace.layer() == TelemetryLayer.CONTROLLER){
      builder.withRequestMetadata(sig.getMethod(), pjp.getArgs());
    }

    if(trace.recordArgs()){
      Object[] args = pjp.getArgs();
      for(int i=0;i<args.length;i++){
        builder.withArgument("arg."+i, args[i]);
      }
    }

    Map<String,String> attrs = builder.build();

    try (ApmSpan span = apm.startSpan(spanName, attrs)) {
      try {
        return pjp.proceed();
      } catch(Throwable t){
        span.recordException(t);
        throw t;
      }
    }
  }
}
