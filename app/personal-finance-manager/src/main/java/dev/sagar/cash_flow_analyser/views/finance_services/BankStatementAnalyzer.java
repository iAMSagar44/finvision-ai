package dev.sagar.cash_flow_analyser.views.finance_services;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.MimeTypeUtils;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.sagar.cash_flow_analyser.commonservice.CategoryService;
import dev.sagar.cash_flow_analyser.dto.FinancialTransaction;
import dev.sagar.cash_flow_analyser.dto.TransactionType;

@Route("/view-bank-statement")
@PageTitle("Bank Statement Analyzer")
@Menu(title = "View Bank Statement", order = 2)
public class BankStatementAnalyzer extends VerticalLayout {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(BankStatementAnalyzer.class);

  public record Transaction(String date, String transactionDetail, String category,
      Double amount) {
  }

  public record Statement(List<Transaction> transactions) {
  }

  private final ChatClient chatClient;

  private final Grid<Transaction> grid = new Grid<>(Transaction.class);

  private List<Transaction> transactions = new ArrayList<>();

  private final CategoryService categoryService;

  private final JdbcTemplate jdbcTemplate;

  private String fileName;

  public BankStatementAnalyzer(
      @Qualifier("geminiOpenaiChatClient") ChatClient geminiChatClient,
      CategoryService categoryService, JdbcTemplate jdbcTemplate) {
    setSizeFull();
    this.chatClient = geminiChatClient;
    this.categoryService = categoryService;
    this.jdbcTemplate = jdbcTemplate;

    // Set up upload
    var buffer = new MemoryBuffer();
    var upload = new Upload(buffer);
    upload.setAcceptedFileTypes(".pdf");
    upload.setMaxFileSize(10 * 1024 * 1024);
    upload.setMaxFiles(1);
    var inputLayout = new HorizontalLayout();
    var saveButton = new Button("Save");
    saveButton.setEnabled(false);
    var clearButton = new Button("Clear");
    clearButton.setEnabled(false);

    upload.addProgressListener(e -> {
      upload.setHeight("200px");
      logger.debug("File upload progress: " + e.getFileName());
    });

    upload.addFailedListener(e -> {
      logger.error("File upload failed: " + e.getFileName());
    });

    upload.addSucceededListener(e -> {
      logger.info("File upload succeeded: " + e.getFileName());
      this.fileName = e.getFileName();
      logger.info("MIME type: " + e.getMIMEType());
      var statement = chatClient.prompt()
          .user(userMessage -> userMessage
              .text(
                  """
                      Please read the attached bank statement and extract all financial transactions.
                      The Transaction Date is in the format DD/MM/YY.
                      Exclude the Transaction Detail named "OPENING BALANCE" and "CLOSING BALANCE".
                      For each transaction, determine the associated category.
                      Select the category from the CATEGORIES section below.
                      If you are unsure about the category, use "Other".
                      Exclude any transaction amount that is less than 0. This indicates a payment or a refund.

                      CATEGORIES (Select one for each transaction):
                      {categories}
                      """)
              .param("categories", this.categoryService.getCategories())
              .media(MimeTypeUtils.parseMimeType(e.getMIMEType()),
                  new InputStreamResource(buffer.getInputStream())))
          .call().entity(Statement.class);

      upload.setHeight("100px");
      showTransactions(statement);
      upload.clearFileList();
      saveButton.setEnabled(true);
      clearButton.setEnabled(true);
      upload.setEnabled(false);
      inputLayout.add(saveButton, clearButton);
    });

    Text instructions =
        new Text("Upload your bank statement to view your financial transactions. "
            + "The transactions will be displayed in a grid below. ");
    add(instructions, upload, inputLayout, createGridLayout());

    saveButton.addClickListener(e -> {
      var items = grid.getGenericDataView().getItems().toList();
      logger.trace("Saving transactions: " + items);
      try {
        saveTransactions(items);
        saveButton.setEnabled(false);
        Notification
            .show("Transactions saved successfully!", 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      } catch (Exception e1) {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.MIDDLE);
        Button closeButton = new Button(new Icon("lumo", "cross"));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.setAriaLabel("Close");
        closeButton.addClickListener(event -> {
          notification.close();
        });
        HorizontalLayout layout = new HorizontalLayout(
            new Div(new Text("Failed to save transactions.")), closeButton);
        layout.setAlignItems(Alignment.CENTER);
        notification.add(layout);
        notification.open();

        logger.error("Error occurred while saving transactions: " + e1.getMessage());
        e1.printStackTrace();
      }
    });

    clearButton.addClickListener(e -> {
      transactions.clear();
      grid.setItems(transactions);
      saveButton.setEnabled(false);
      clearButton.setEnabled(false);
      upload.setEnabled(true);
      inputLayout.remove(saveButton, clearButton);
    });

  }

  @Transactional
  private void saveTransactions(List<Transaction> transactions) throws Exception {
    try {
      List<FinancialTransaction> financialTransactions = transactions.stream().map(t -> {
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("[d/M/yy][dd/MM/yy][d/MM/yyyy]");
        LocalDate date = LocalDate.parse(t.date(), formatter);
        return new FinancialTransaction(date, t.amount(), t.transactionDetail(),
            t.category(), TransactionType.DEBIT);
      }).toList();

      String sql =
          """
              INSERT INTO financial_transactions (date, amount, transaction_detail, category, transaction_type, source_file)
              VALUES (?, ?, ?, ?, ?, ?)
              """;

      int[][] batchResults =
          jdbcTemplate.batchUpdate(sql, financialTransactions, 50, (ps, transaction) -> {
            ps.setDate(1, Date.valueOf(transaction.date()));
            ps.setDouble(2, transaction.amount());
            ps.setString(3, transaction.transaction_detail());
            ps.setString(4, transaction.category());
            ps.setString(5, transaction.transaction_type().name());
            ps.setString(6, this.fileName);
          });

      // Verify all batches were successful
      for (int i = 0; i < batchResults.length; i++) {
        for (int j = 0; j < batchResults[i].length; j++) {
          if (batchResults[i][j] == 0) {
            throw new Exception(
                "Failed to insert transaction at batch " + i + ", position " + j);
          }
        }
      }

      logger.info("Transactions saved successfully.");
    } catch (Exception ex) {
      logger.error("Error saving transactions: " + ex.getMessage(), ex);
      throw new Exception("Failed to save transactions - rolling back", ex);
    }
  }

  private VerticalLayout createGridLayout() {
    VerticalLayout gridContainer = new VerticalLayout();
    grid.setItems(transactions);
    grid.setColumns("date", "transactionDetail", "category", "amount");
    grid.setSortableColumns("transactionDetail", "category", "amount");
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    gridContainer.addAndExpand(grid);
    return gridContainer;
  }

  private void showTransactions(Statement statement) {
    logger.debug("Displaying transactions: " + statement.transactions());
    if (statement != null && statement.transactions() != null) {
      transactions = new ArrayList<>(statement.transactions());
      logger.info("{} transactions found", transactions.size());
      grid.setItems(transactions);
    }
  }

}
