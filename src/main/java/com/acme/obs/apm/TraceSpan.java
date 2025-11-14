package com.acme.obs.apm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TraceSpan {
  String value() default "";
  String[] attributes() default {};
  boolean recordArgs() default false;
  TelemetryLayer layer() default TelemetryLayer.INTERNAL;
}
