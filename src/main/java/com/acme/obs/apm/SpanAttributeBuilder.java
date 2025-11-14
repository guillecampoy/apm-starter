package com.acme.obs.apm;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder orientado a objetos para construir mapas de atributos de spans.
 * Extrae metadatos del componente (clase/método), capa lógica y anotaciones web.
 */
public final class SpanAttributeBuilder {
  private final Map<String,String> attributes = new HashMap<>();

  public static SpanAttributeBuilder create(){ return new SpanAttributeBuilder(); }

  private SpanAttributeBuilder(){}

  public SpanAttributeBuilder withLayer(TelemetryLayer layer){
    if(layer!=null){
      attributes.put("component.layer", layer.attributeValue());
    }
    return this;
  }

  public SpanAttributeBuilder withComponentDetails(Class<?> type){
    if(type!=null){
      attributes.put("component.class", type.getName());
      attributes.put("component.simple", type.getSimpleName());
      attributes.put("component.package", type.getPackageName());
    }
    return this;
  }

  public SpanAttributeBuilder withMethod(Method method){
    if(method!=null){
      attributes.put("component.method", method.getName());
    }
    return this;
  }

  public SpanAttributeBuilder withDeclaredAttributes(String[] kvPairs){
    if(kvPairs==null) return this;
    for(String kv : kvPairs){
      if(!StringUtils.hasText(kv)) continue;
      int idx = kv.indexOf('=');
      if(idx <= 0 || idx == kv.length()-1) continue;
      attributes.put(kv.substring(0, idx), kv.substring(idx+1));
    }
    return this;
  }

  public SpanAttributeBuilder withRequestMetadata(Method method, Object[] args){
    if(method == null) return this;
    Parameter[] parameters = method.getParameters();
    if(parameters.length == 0) return this;
    for(int i=0; i<parameters.length; i++){
      Parameter parameter = parameters[i];
      Object value = args != null && args.length>i? args[i] : null;
      PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
      if(pathVariable != null){
        String name = firstNonBlank(pathVariable.value(), pathVariable.name(), parameter.getName());
        attributes.put("http.path."+name, safeValue(value));
      }
      RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
      if(requestParam != null){
        String name = firstNonBlank(requestParam.value(), requestParam.name(), parameter.getName());
        attributes.put("http.param."+name, safeValue(value));
      }
    }
    return this;
  }

  public SpanAttributeBuilder withArgument(String key, Object value){
    if(StringUtils.hasText(key)){
      attributes.put(key, safeValue(value));
    }
    return this;
  }

  public Map<String,String> build(){
    return new HashMap<>(attributes);
  }

  private static String firstNonBlank(String... values){
    for(String v : values){
      if(StringUtils.hasText(v)) return v;
    }
    return "value";
  }

  private static String safeValue(Object value){
    if(value == null) return "null";
    String text = String.valueOf(value);
    return text.length() > 80 ? text.substring(0,80)+"..." : text;
  }
}
