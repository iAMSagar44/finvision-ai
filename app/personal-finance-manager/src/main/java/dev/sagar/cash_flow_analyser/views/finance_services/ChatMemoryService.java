package dev.sagar.cash_flow_analyser.views.finance_services;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Content;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A simplified chat message storage service that maintains conversation history. This class
 * provides functionality to store and retrieve chat messages from a ChatMemory instance.
 */
class ChatMemoryService {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(ChatMemoryService.class);

  private final ChatMemory chatMemory;
  private final String conversationId;
  private final int windowSize;


  /**
   * The default conversation id to use when no conversation id is provided.
   */
  public static final String DEFAULT_CHAT_MEMORY_CONVERSATION_ID = "cash-flow-analyser";

  /**
   * The default chat memory retrieve size to use when no retrieve size is provided.
   */
  public static final int DEFAULT_CHAT_MEMORY_RESPONSE_SIZE = 50;

  /**
   * Creates a new ChatMessageStore with provided ChatMemory and default settings
   */
  public ChatMemoryService(ChatMemory chatMemory) {
    this(chatMemory, DEFAULT_CHAT_MEMORY_CONVERSATION_ID, DEFAULT_CHAT_MEMORY_RESPONSE_SIZE);
  }

  /**
   * Creates a new ChatMessageStore with specified settings
   * 
   * @param chatMemory the chat memory instance to use
   * @param defaultConversationId the default conversation ID to use
   * @param windowSize the default number of messages to keep in history
   */
  public ChatMemoryService(ChatMemory chatMemory, int windowSize) {
    this(chatMemory, DEFAULT_CHAT_MEMORY_CONVERSATION_ID, windowSize);
  }

  /**
   * Creates a new ChatMessageStore with specified settings
   * 
   * @param chatMemory the chat memory instance to use
   * @param conversationId the default conversation ID to use
   * @param windowSize the default number of messages to keep in history
   */
  public ChatMemoryService(ChatMemory chatMemory, String conversationId, int windowSize) {
    this.chatMemory = chatMemory;
    this.conversationId = conversationId;
    this.windowSize = windowSize;
  }

  /**
   * Adds a message to the specified conversation
   * 
   * @param message the message to add
   */
  private void add(Message message) {
    add(conversationId, message);
  }

  /**
   * Adds a message to the specified conversation
   * 
   * @param conversationId the conversation ID
   * @param message the message to add
   */
  private void add(String conversationId, Message message) {
    chatMemory.add(conversationId, message);
  }


  /**
   * Gets all user and assistant messages from the conversation
   * 
   * @return list of messages filtered by USER and ASSISTANT types
   */
  public String getConversationMessages() {

    List<Message> memoryMessages = getConversationMessages(conversationId);

    String memory = (memoryMessages != null) ? memoryMessages.stream().filter(
        m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
        .map(m -> m.getMessageType() + ":" + ((Content) m).getText())
        .collect(Collectors.joining(System.lineSeparator())) : "";

    logger.debug("Chat Memory current size: {} \n Messages in memory for conversation id {}: {}",
        memoryMessages.size(), getConversationId(), memory);
    return memory;
  }

  public List<Message> getMemoryMessages() {
    return getConversationMessages(conversationId);
  }

  /**
   * Gets all user and assistant messages from the specified conversation
   * 
   * @param conversationId the conversation ID
   * @param limit maximum number of messages to return
   * @return list of messages filtered by USER and ASSISTANT types
   */
  private List<Message> getConversationMessages(String conversationId) {
    return chatMemory.get(conversationId).stream().filter(
        m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
        .toList();
  }

  /**
   * Clear all messages in all conversations
   */
  public void clearAll() {
    chatMemory.clear(conversationId);
  }

  public String getConversationId() {
    return conversationId;
  }

  public int getWindowSize() {
    return windowSize;
  }

  public ChatMemory getChatMemoryType() {
    return chatMemory;
  }

  public Flux<String> addMessages(Flux<String> chatMessageStream, String userQuestion) {
    AtomicReference<StringBuilder> messageTextContentRef =
        new AtomicReference<>(new StringBuilder());

    return chatMessageStream.doOnSubscribe(subscription -> {
      logger.info("Subscription started...starting to aggregate messages");
      messageTextContentRef.set(new StringBuilder());
    }).doOnNext(token -> {
      if (token != null && !token.startsWith("<small>")) {
        messageTextContentRef.get().append(token);
      }
    }).doOnComplete(() -> {
      logger.info("Subscription completed...aggregated messages");
      logger.info("Adding user message to chat memory");
      add(new UserMessage(userQuestion));
      String messageContent = messageTextContentRef.get().toString().trim();
      if (!messageContent.isEmpty()) {
        logger.info("Adding assistant message to chat memory");
        add(new AssistantMessage(messageContent));
      }
    });
  }
}
