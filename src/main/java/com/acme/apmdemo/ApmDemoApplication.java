package com.acme.apmdemo;

import com.acme.obs.apm.ApmClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {"com.acme"})
public class ApmDemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(ApmDemoApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate(ApmClient apm){
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add((req, body, exec) -> {
      apm.injectContext((k,v)-> req.getHeaders().add(k,v));
      try (var span = apm.startSpan("HTTP OUT "+req.getMethod()+" "+req.getURI(), java.util.Map.of("otel.kind","client"))){
        var resp = exec.execute(req, body);
        span.setAttribute("http.status_code", resp.getStatusCode().value());
        return resp;
      } catch (Exception e){
        apm.recordException(e);
        throw e;
      }
    });
    return rt;
  }
}
