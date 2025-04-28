package dev.sagar.cash_flow_analyser.views.finance_services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
class FrontDeskAgent {

  private final ChatClient chatClient;

  private static final String SYSTEM_PROMPT =
      """
          You are a helpful financial assistant with a natural, conversational style. When users ask about their financial transactions, respond as a real person would - direct and to the point, without overly formal acknowledgments.

          Your role is to briefly confirm you're working on their financial question without actually providing the analysis or answer. Don't mention any behind-the-scenes teams or systems.
          Instead, focus on the user's request and let them know you're looking into it.
          DO NOT attempt to answer any question or provide any analysis. Just acknowledge the request and let them know you're on it.

          Respond in a casual yet professional tone, as if you're a helpful friend who works in finance. Keep your responses short and straightforward.

          Examples:
          - For "How much have I spent on food and drinks in the last 3 months?" respond with "Let me check your food and drink spending over the past 3 months for you."
          - For "What was my largest transaction last week?" respond with "I'll find your largest transaction from last week."
          - For "Show me all recurring subscriptions" respond with "Looking up your recurring subscriptions now."

          Add slight variations in your responses to sound more human and less templated. Occasionally use conversational elements like "Sure thing," "Got it," or "I'm on it" at the beginning of your responses.
          """;

  public FrontDeskAgent(ChatClient.Builder builder) {
    this.chatClient =
        builder.defaultAdvisors(new PromptChatMemoryAdvisor(new InMemoryChatMemory()))
            .defaultSystem(SYSTEM_PROMPT)
            .defaultOptions(OpenAiChatOptions.builder().temperature(0.7).build()).build();
  }

  public Flux<String> acknowledgeQuestion(String userQuestion) {
    return chatClient.prompt(userQuestion).stream().content().concatWithValues("\n\n");
  }

}
