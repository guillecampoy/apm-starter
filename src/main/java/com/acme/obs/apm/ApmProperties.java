package com.acme.obs.apm;

import java.util.Map;

public class ApmProperties {
  private String vendor;
  private String applicationName;
  private String serviceName;
  private String serviceVersion;
  private String environment;
  private String applicationId;
  private String accountId;
  private String apiKey;
  private String endpoint;
  private Map<String,String> defaultTags;

  public String getVendor() { return vendor; }
  public void setVendor(String vendor) { this.vendor = vendor; }
  public String getApplicationName() { return applicationName; }
  public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
  public String getServiceName() { return serviceName; }
  public void setServiceName(String serviceName) { this.serviceName = serviceName; }
  public String getServiceVersion() { return serviceVersion; }
  public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }
  public String getEnvironment() { return environment; }
  public void setEnvironment(String environment) { this.environment = environment; }
  public String getApplicationId() { return applicationId; }
  public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
  public String getAccountId() { return accountId; }
  public void setAccountId(String accountId) { this.accountId = accountId; }
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) { this.apiKey = apiKey; }
  public String getEndpoint() { return endpoint; }
  public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
  public Map<String, String> getDefaultTags() { return defaultTags; }
  public void setDefaultTags(Map<String, String> defaultTags) { this.defaultTags = defaultTags; }
}
