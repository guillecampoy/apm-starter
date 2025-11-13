package com.acme.obs.apm;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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

    Map<String,String> attrs = new HashMap<>();
    for(String kv : trace.attributes()){
      int i = kv.indexOf('=');
      if(i>0) attrs.put(kv.substring(0,i), kv.substring(i+1));
    }
    attrs.put("class", sig.getDeclaringTypeName());
    attrs.put("method", sig.getMethod().getName());

    if(trace.recordArgs()){
      Object[] args = pjp.getArgs();
      for(int i=0;i<args.length;i++){
        attrs.put("arg."+i, String.valueOf(args[i]));
      }
    }

    try (ApmSpan span = apm.startSpan(spanName, attrs)) {
      Object out = pjp.proceed();
      return out;
    } catch(Throwable t){
      apm.recordException(t);
      throw t;
    }
  }
}
