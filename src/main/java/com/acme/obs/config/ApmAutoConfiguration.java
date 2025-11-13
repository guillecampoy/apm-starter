package com.acme.obs.config;

import com.acme.obs.apm.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableConfigurationProperties
public class ApmAutoConfiguration {

  @Bean
  @ConfigurationProperties(prefix="observability.apm")
  public ApmProperties apmProperties(){ return new ApmProperties(); }

  @Bean
  @ConditionalOnMissingBean(ApmClient.class)
  public ApmClient apmClient(ApmProperties props){
    String vendor = props.getVendor()!=null? props.getVendor().toLowerCase() : "otel";
    switch (vendor){
      case "otel": default: return new OtelApmClient(props);
    }
  }

  @Bean
  public FilterRegistrationBean<WebTracingFilter> webTracingFilter(ApmClient apm){
    FilterRegistrationBean<WebTracingFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new WebTracingFilter(apm));
    reg.setOrder(1);
    return reg;
  }
}
