package dev.sagar.cash_flow_analyser.views.finance_services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import jakarta.annotation.Nullable;

@Service
class SQLQueryAgent {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(SQLQueryAgent.class);

  @Value("classpath:/prompts/schema.st")
  private Resource ddlResource;

  @Value("${app.database.type}")
  private String databaseType;

  @Value("classpath:/prompts/sql-prompt-template.st")
  private Resource sqlPromptTemplateResource;

  private final ChatClient chatClient;

  private final DatabaseService databaseService;

  public SQLQueryAgent(ChatClient.Builder builder, DatabaseService databaseService) {
    this.chatClient = builder.build();
    this.databaseService = databaseService;

  }

  @Tool(description = "Generate a SQL query based on the user's question")
  public String generateQuery(@ToolParam(
      description = "The rewritten question to generate a SQL query for") String question,
      @ToolParam(
          description = "The feedback from the evaluateQuery tool") @Nullable String feedback,
      @ToolParam(
          description = "The SQL query for which the feedback was received") @Nullable String sqlQuery)
      throws IOException {
    String schema = ddlResource.getContentAsString(Charset.defaultCharset());

    logger.debug("The schema is: {}", schema);
    if (feedback.isBlank() || feedback.isEmpty()) {
      String query = generateSQL(question, schema, "No feedback at this point");
      return query;
    } else {
      logger.debug("Feedback received {}", feedback);
      StringBuilder newContext = new StringBuilder();
      newContext.append("\nPrevious attempts:");

      newContext.append("\n- ").append(sqlQuery);
      newContext.append("\nFeedback: ").append(feedback);
      String query = generateSQL(question, schema, newContext.toString());
      return query;
    }
  }

  private String generateSQL(String question, String schema, String feedback) {
    logger.trace("Retrieving categories from the database.");
    var categories = String.join(", ", databaseService.getCategories());
    return chatClient.prompt().user(userSpec -> userSpec.text(sqlPromptTemplateResource)
        .param("database_type", databaseType).param("question", question)
        .param("current_date",
            LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
        .param("ddl", schema).param("categories", categories).param("feedback", feedback))
        .call().content();
  }

}
