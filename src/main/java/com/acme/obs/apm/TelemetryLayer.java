package com.acme.obs.apm;

/**
 * Identifica la capa lógica sobre la cual se crea un span.
 * Facilita agregar atributos consistentes de forma dinámica.
 */
public enum TelemetryLayer {
  CONTROLLER,
  SERVICE,
  REPOSITORY,
  DATABASE,
  HTTP_CLIENT,
  EXTERNAL,
  INTERNAL;

  public String attributeValue() {
    return name().toLowerCase();
  }
}
