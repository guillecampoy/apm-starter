package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import com.acme.obs.apm.TelemetryLayer;
import com.acme.obs.apm.TraceSpan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class ExternalController {
  private final RestTemplate rt;
  private final ApmClient apm;

  public ExternalController(RestTemplate rt, ApmClient apm){ this.rt=rt; this.apm=apm; }

  @GetMapping("/external/ping")
  @TraceSpan(value="external_ping", layer=TelemetryLayer.CONTROLLER)
  public ResponseEntity<String> ping(){
    String body = rt.getForObject("http://localhost:8089/__admin/mappings", String.class);
    int size = body != null? body.length() : 0;
    apm.addEvent("external.ping.response", Map.of("payload.size", String.valueOf(size)));
    return ResponseEntity.ok("ok:" + size);
  }
}
