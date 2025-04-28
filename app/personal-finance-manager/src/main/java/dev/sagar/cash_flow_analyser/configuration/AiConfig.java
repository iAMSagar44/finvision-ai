package dev.sagar.cash_flow_analyser.configuration;

import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeminiAiProperties.class)
public class AiConfig {

  @Bean
  public ChatMemory chatMemory() {
    return new InMemoryChatMemory();
  }

  @Bean(name = "geminiOpenaiChatClient")
  public ChatClient geminiOpenaiChatClient(
      GeminiAiProperties geminiAiProperties) {
    ReactorClientHttpRequestFactory requestFactory =
        new ReactorClientHttpRequestFactory();
    requestFactory.setReadTimeout(
        Duration.ofSeconds(geminiAiProperties.getReadTimeout()));
    final RestClient.Builder restClientBuilder =
        RestClient.builder().requestFactory(requestFactory);

    return ChatClient.builder(OpenAiChatModel.builder()
        .openAiApi(OpenAiApi.builder().apiKey(geminiAiProperties.getApiKey())
            .baseUrl(geminiAiProperties.getBaseUrl())
            .completionsPath(geminiAiProperties.getCompletionsPath())
            .restClientBuilder(restClientBuilder).build())
        .defaultOptions(OpenAiChatOptions.builder()
            .model(geminiAiProperties.getModel()).temperature(0.7).build())
        .build()).build();

  }

}
