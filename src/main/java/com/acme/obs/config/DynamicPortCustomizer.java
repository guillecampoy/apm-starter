package com.acme.obs.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

/**
 * Evita fallos por puertos ocupados buscando dinámicamente un puerto libre.
 */
@Component
public class DynamicPortCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
  private static final Logger log = LoggerFactory.getLogger(DynamicPortCustomizer.class);

  private final int preferredPort;
  private final int fallbackAttempts;

  public DynamicPortCustomizer(
      @Value("${server.port:8080}") int preferredPort,
      @Value("${app.server.port-fallback-attempts:20}") int fallbackAttempts
  ) {
    this.preferredPort = preferredPort;
    this.fallbackAttempts = Math.max(0, fallbackAttempts);
  }

  @Override
  public void customize(ConfigurableServletWebServerFactory factory) {
    int portToUse = preferredPort;
    if (!isPortAvailable(preferredPort)) {
      portToUse = findAvailablePort(preferredPort + 1, preferredPort + fallbackAttempts);
      if (portToUse == -1) {
        log.error("No se encontró puerto libre tras {} intentos; se elegirá un puerto aleatorio del SO", fallbackAttempts);
        portToUse = 0; // Spring Boot pedirá un puerto ephemeral
      }
      log.warn("Puerto {} ocupado. Se usará dinámicamente {}", preferredPort, portToUse);
    }
    factory.setPort(portToUse);
  }

  private boolean isPortAvailable(int port) {
    try (ServerSocket socket = new ServerSocket()) {
      socket.setReuseAddress(false);
      socket.bind(new InetSocketAddress("localhost", port), 1);
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  private int findAvailablePort(int start, int end) {
    if (start < 1024) start = 1024;
    for (int port = start; port <= end; port++) {
      if (isPortAvailable(port)) {
        return port;
      }
    }
    return -1;
  }
}
