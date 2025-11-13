package com.acme.obs.apm;

import java.util.Map;

public interface ApmClient {
  ApmSpan startSpan(String name);
  ApmSpan startSpan(String name, Map<String, String> attributes);
  void addEvent(String name, Map<String, String> attributes);
  void recordException(Throwable t);
  void setTransactionName(String name);
  void setUser(String userId, Map<String,String> attrs);
  void incrementCounter(String name, double amount, Map<String,String> tags);
  void recordHistogram(String name, double value, Map<String,String> tags);
  void gauge(String name, double value, Map<String,String> tags);
  void injectContext(java.util.function.BiConsumer<String,String> headerSetter);
  void extractContext(java.util.function.Function<String,String> headerGetter);
}
