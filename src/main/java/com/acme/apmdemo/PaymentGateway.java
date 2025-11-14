package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import com.acme.obs.apm.TelemetryLayer;
import com.acme.obs.apm.TraceSpan;
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

  @TraceSpan(value="payment_gateway_enqueue", layer=TelemetryLayer.HTTP_CLIENT)
  public void enqueueTransfer(Map<String, Object> instruction){
    String url = "http://localhost:8089/payments";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String,Object>> req = new HttpEntity<>(instruction, headers);
    var resp = rt.postForEntity(url, req, String.class);
    apm.setAttribute("external.status_code", resp.getStatusCode().value());
    apm.setAttribute("external.url", url);
    apm.setAttribute("payment.loanId", String.valueOf(instruction.get("loanId")));
  }
}
