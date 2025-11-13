package com.acme.obs.apm;

public interface ApmSpan extends AutoCloseable {
  ApmSpan setAttribute(String key, String value);
  ApmSpan setAttribute(String key, long value);
  ApmSpan setAttribute(String key, double value);
  ApmSpan recordException(Throwable t);
  @Override void close();
}
