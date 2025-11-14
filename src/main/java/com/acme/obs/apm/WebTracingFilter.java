package com.acme.obs.apm;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WebTracingFilter extends OncePerRequestFilter {
  private final ApmClient apm;
  public WebTracingFilter(ApmClient apm){ this.apm = apm; }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    String reqId = Optional.ofNullable(req.getHeader("X-Request-Id"))
                           .orElse(UUID.randomUUID().toString());
    MDC.put("requestId", reqId);

    ApmScope extracted = apm.extractContext(req::getHeader);
    java.util.Map<String,String> attrs = new java.util.HashMap<>(Map.of(
      "http.method", req.getMethod(),
      "http.target", req.getRequestURI(),
      "request.id", reqId
    ));
    attrs.put("otel.kind", "server");

    String displayedName = req.getMethod()+" "+req.getRequestURI();

    try (extracted; ApmSpan span = apm.startSpan("HTTP " + displayedName, attrs)) {
      String pattern = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      if (pattern != null) {
        apm.setTransactionName(req.getMethod() + " " + pattern);
        span.setAttribute("http.route", pattern);
      } else {
        apm.setTransactionName(displayedName);
      }
      try {
        chain.doFilter(req, res);
        span.setAttribute("http.status_code", res.getStatus());
      } catch (Throwable t){
        span.recordException(t);
        throw t;
      }
    } finally {
      MDC.remove("requestId");
    }
  }
}
