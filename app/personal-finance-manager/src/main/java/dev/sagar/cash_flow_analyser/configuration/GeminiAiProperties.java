package dev.sagar.cash_flow_analyser.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.ai.openai")
public class GeminiAiProperties {
  private String apiKey;
  private String model;
  private String baseUrl;
  private String completionsPath;
  private long readTimeout;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModel() {
    return model;
  }

  public void setReadTimeout(long readTimeout) {
    this.readTimeout = readTimeout;
  }

  public long getReadTimeout() {
    return readTimeout;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getCompletionsPath() {
    return completionsPath;
  }

  public void setCompletionsPath(String completionsPath) {
    this.completionsPath = completionsPath;
  }
}
