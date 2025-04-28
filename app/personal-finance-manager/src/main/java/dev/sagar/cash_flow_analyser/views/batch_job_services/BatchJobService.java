package dev.sagar.cash_flow_analyser.views.batch_job_services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.stereotype.Service;

@Service
class BatchJobService {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(BatchJobService.class);

  private final JobLauncher jobLauncher;

  private final Job job;

  private final JobExplorer jobExplorer;

  private final JobOperator jobOperator;

  public BatchJobService(JobLauncher jobLauncher, Job job, JobExplorer jobExplorer,
      JobOperator jobOperator) {
    this.job = job;
    this.jobLauncher = jobLauncher;
    this.jobExplorer = jobExplorer;
    this.jobOperator = jobOperator;
  }

  @Tool(
      description = "Retrieves the status of a batch job based on the job execution ID.")
  public String getBatchJobStatus(
      @ToolParam(description = "The job execution ID") Long jobExecutionId)
      throws Exception {
    logger.info("Fetching job details for execution ID: {}", jobExecutionId);

    JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);

    if (execution == null) {
      throw new NoSuchJobExecutionException(
          "No JobExecution found for id: [" + jobExecutionId + "]");
    }

    var jobDetails = new JobDetails(execution.getStatus().name(), execution.getId(),
        execution.getJobInstance().getInstanceId(),
        execution.getJobInstance().getJobName(), execution.getCreateTime(),
        execution.getEndTime(), execution.getLastUpdated());

    logger.info("Job details: {}", jobDetails);
    return jobDetails.toString();
  }

  @Tool(description = "Triggers a batch job based on the provided file name.")
  public String triggerBatchJobs(
      @ToolParam(description = "The name of the file") String fileName) throws Exception {
    logger.info("Triggering batch job: {}", fileName);

    Path filePath;
    try {
      filePath = getFilePath(fileName);
    } catch (FileNotFoundException e) {
      logger.error("Error during file retrieval {}", e.getClass());
      e.printStackTrace();
      return new JobDetails("File not found", -1L, -1L, "", null).toString();
    } catch (IOException e) {
      logger.error("Error during file retrieval {}", e.getClass());
      e.printStackTrace();
      return new JobDetails("IO error occurred", -1L, -1L, "", null).toString();
    }

    JobExecution execution = jobLauncher.run(job, new JobParametersBuilder()
        .addString("fileName", filePath.toAbsolutePath().toString()).toJobParameters());


    var jobDetails = new JobDetails(execution.getStatus().name(), execution.getId(),
        execution.getJobInstance().getInstanceId(),
        execution.getJobInstance().getJobName(), execution.getCreateTime());

    logger.info("Batch job triggered successfully: {}", jobDetails);
    return jobDetails.toString();
  }

  @Tool(description = "Restarts a failed job based on the job execution ID.")
  public String restartBatchJob(
      @ToolParam(description = "The job execution ID") Long jobExecutionId)
      throws Exception {
    logger.info("Restarting batch job for execution ID: {}", jobExecutionId);

    JobExecution lastExecution = jobExplorer.getJobExecution(jobExecutionId);

    if (lastExecution != null && (lastExecution.getStatus() == BatchStatus.FAILED
        || lastExecution.getStatus() == BatchStatus.STOPPED)) {

      Long restartId = jobOperator.restart(jobExecutionId);
      JobExecution currentExecution = jobExplorer.getJobExecution(restartId);
      var jobDetails = new JobDetails(currentExecution.getStatus().name(),
          currentExecution.getId(), currentExecution.getJobInstance().getInstanceId(),
          currentExecution.getJobInstance().getJobName(),
          currentExecution.getCreateTime()).toString();
      logger.info("Batch job restarted successfully: {}", jobDetails);
      return jobDetails;
    }

    if (lastExecution == null) {
      logger.error("No JobExecution found for id: {}", jobExecutionId);
      return "No JobExecution found";
    }
    return "No JobExecution found for id: " + jobExecutionId;
  }

  private Path getFilePath(String fileName) throws IOException {
    String userHome = System.getProperty("user.home");
    Path filePath = Paths.get(userHome, "Downloads", fileName);
    if (!filePath.toFile().exists()) {
      throw new FileNotFoundException();
    }
    return filePath;
  }

}
