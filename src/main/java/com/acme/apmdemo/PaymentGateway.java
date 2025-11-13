package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PaymentGateway {
  private final RestTemplate rt;
  private final ApmClient apm;
  public PaymentGateway(RestTemplate rt, ApmClient apm){ this.rt=rt; this.apm=apm; }

  public void enqueueTransfer(Map<String, Object> instruction){
    String url = "http://localhost:8089/payments";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String,Object>> req = new HttpEntity<>(instruction, headers);
    try (var span = apm.startSpan("gateway.enqueueTransfer", java.util.Map.of("otel.kind","client"))){
      var resp = rt.postForEntity(url, req, String.class);
      span.setAttribute("external.status", resp.getStatusCode().value());
    }
  }
}
