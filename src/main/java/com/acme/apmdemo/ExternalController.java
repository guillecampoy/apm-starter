package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ExternalController {
  private final RestTemplate rt;
  private final ApmClient apm;

  public ExternalController(RestTemplate rt, ApmClient apm){ this.rt=rt; this.apm=apm; }

  @GetMapping("/external/ping")
  public ResponseEntity<String> ping(){
    try (var s = apm.startSpan("external_ping")){
      String body = rt.getForObject("http://localhost:8089/__admin/mappings", String.class);
      return ResponseEntity.ok("ok:" + (body!=null? body.length():0));
    }
  }
}
