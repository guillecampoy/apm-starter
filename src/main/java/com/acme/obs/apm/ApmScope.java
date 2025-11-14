package com.acme.obs.apm;

/**
 * Wrapper liviano para scopes de telemetría que no arrojan checked exceptions al cerrarse.
 */
@FunctionalInterface
public interface ApmScope extends AutoCloseable {
  @Override
  void close();

  static ApmScope noop() {
    return () -> {};
  }
}
