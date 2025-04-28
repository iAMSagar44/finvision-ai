package dev.sagar.cash_flow_analyser.views.batch_job_services;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.PathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import dev.sagar.cash_flow_analyser.dto.FinancialTransaction;

/**
 * Configures and returns a Spring Batch Step for processing financial transactions. This step reads
 * data from a CSV file, processes it to filter out credit transactions, and writes the resulting
 * data to a database table.
 *
 * @param jobRepository the JobRepository to manage job execution
 * @param transactionManager the transaction manager to handle transactions
 * @param itemReader the ItemReader to read financial transactions from the input source
 * @param itemProcessor the ItemProcessor to process and filter financial transactions
 * @param itemWriter the ItemWriter to write processed transactions to the output destination
 */
@EnableBatchProcessing
@Configuration
public class BatchConfig {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(BatchConfig.class);

  @Bean
  Job job(JobRepository jobRepository, Step step1) {
    return new JobBuilder("load-transactions", jobRepository).start(step1).build();
  }

  @Bean
  Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
      ItemReader<FinancialTransaction> itemReader,
      ItemProcessor<FinancialTransaction, FinancialTransaction> itemProcessor,
      ItemWriter<FinancialTransaction> itemWriter,
      CompositeItemProcessor<FinancialTransaction, FinancialTransaction> compositeProcessor) {

    return new StepBuilder("read-process-write-step", jobRepository)
        .<FinancialTransaction, FinancialTransaction>chunk(10, transactionManager)
        .reader(itemReader).processor(compositeProcessor).writer(itemWriter).build();
  }

  @StepScope
  @Bean
  FlatFileItemReader<FinancialTransaction> itemReader(
      @Value("#{jobParameters['fileName']}") String name) {
    logger.info("Reading file in Item Reader: {}", name);
    return new FlatFileItemReaderBuilder<FinancialTransaction>()
        .name("financialTransactionItemReader").resource(new PathResource(name))
        .delimited().delimiter(",")
        .names(new String[] {"transactionDate", "amount", "description", "balance"})
        .fieldSetMapper(fieldSet -> new FinancialTransaction(
            LocalDate.parse(fieldSet.readString("transactionDate"),
                DateTimeFormatter
                    .ofPattern("[dd/MM/yyyy][d/M/yyyy][d/MM/yyyy][dd/MM/yyyy]")),
            fieldSet.readDouble("amount"), fieldSet.readString("description")))
        .saveState(true).build();
  }

  @StepScope
  @Bean
  @Primary
  CompositeItemProcessor<FinancialTransaction, FinancialTransaction> compositeProcessor(
      CategorizeTransactionProcessor categorizeTransactionProcessor,
      DataSource dataSource, @Value("#{jobParameters['fileName']}") String name) {
    List<ItemProcessor<FinancialTransaction, FinancialTransaction>> delegates =
        new ArrayList<>(3);
    delegates.add(new FilterCreditTransactionsProcessor(dataSource, name));
    delegates.add(new FilterCreditCardPaymentsProcessor(dataSource, name));
    delegates.add(categorizeTransactionProcessor);

    CompositeItemProcessor<FinancialTransaction, FinancialTransaction> processor =
        new CompositeItemProcessor<>();

    processor.setDelegates(delegates);

    return processor;
  }

  @StepScope
  @Bean
  JdbcBatchItemWriter<FinancialTransaction> itemWriter(DataSource dataSource,
      @Value("#{jobParameters['fileName']}") String name) {
    return new JdbcBatchItemWriterBuilder<FinancialTransaction>().dataSource(dataSource)
        .sql(
            """
                INSERT INTO financial_transactions (date, amount, transaction_detail, category, transaction_type, source_file)
                VALUES (?, ?, ?, ?, ?, ?)
                """)
        .itemPreparedStatementSetter((item, ps) -> {
          ps.setDate(1, Date.valueOf(item.date()));
          ps.setDouble(2, item.amount());
          ps.setString(3, item.transaction_detail());
          ps.setString(4, item.category());
          ps.setString(5, item.transaction_type().name());
          ps.setString(6, name);
        }).build();
  }

  /**
   * Configures and provides a JobLauncher bean for launching batch jobs.
   *
   * This method creates an instance of TaskExecutorJobLauncher, sets the provided JobRepository, and
   * assigns a SimpleAsyncTaskExecutor to handle task execution asynchronously. It ensures that all
   * required properties are set before returning the configured JobLauncher instance.
   * 
   * @param jobRepository the JobRepository to be used by the JobLauncher for managing job executions
   * @return a fully configured JobLauncher instance
   * @throws Exception if there is an error during the initialization of the JobLauncher
   */
  @Bean
  JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
    TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

}
