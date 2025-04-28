package dev.sagar.batch_job_mcp.job;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class BatchJobService {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BatchJobService.class);

	private final JobLauncher asyncJobLauncher;

	private final Job job;

	private final JobExplorer jobExplorer;

	private final JobOperator jobOperator;

	public BatchJobService(Job job, @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
			JobExplorer jobExplorer, JobOperator jobOperator) {
		this.job = job;
		this.asyncJobLauncher = asyncJobLauncher;
		this.jobExplorer = jobExplorer;
		this.jobOperator = jobOperator;
	}

	@Tool(description = "Triggers a batch job based on the provided file name.")
	public ResponseEntity<JobDetails> startJob(@ToolParam(description = "The name of the file") String fileName)
			throws Exception {

		Path filePath;
		try {
			filePath = getFilePath(fileName);
		}
		catch (FileNotFoundException e) {
			logger.error("Error during file retrieval {}", e.getClass());
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new JobDetails("File not found", -1L, -1L, "", null));
		}
		catch (IOException e) {
			logger.error("Error during file retrieval {}", e.getClass());
			e.printStackTrace();
			return ResponseEntity.badRequest().body(new JobDetails("IO error occurred", -1L, -1L, "", null));
		}

		JobExecution execution = asyncJobLauncher.run(job,
				new JobParametersBuilder().addString("fileName", filePath.toAbsolutePath().toString())
					.toJobParameters());
		return ResponseEntity.ok(new JobDetails(execution.getStatus().name(), execution.getId(),
				execution.getJobInstance().getInstanceId(), execution.getJobInstance().getJobName(),
				execution.getCreateTime()));
	}

	@Tool(description = "Restarts a failed job based on the job execution ID.")
	public ResponseEntity<?> restartJob(@ToolParam(description = "The job execution ID") Long executionId)
			throws Exception {
		JobExecution lastExecution = jobExplorer.getJobExecution(executionId);

		if (lastExecution != null && (lastExecution.getStatus() == BatchStatus.FAILED
				|| lastExecution.getStatus() == BatchStatus.STOPPED)) {

			Long restartId = jobOperator.restart(executionId);
			JobExecution currentExecution = jobExplorer.getJobExecution(restartId);
			return ResponseEntity.ok(new JobDetails(currentExecution.getStatus().name(), currentExecution.getId(),
					currentExecution.getJobInstance().getInstanceId(), currentExecution.getJobInstance().getJobName(),
					currentExecution.getCreateTime()));
		}

		if (lastExecution == null) {
			logger.error("No JobExecution found for id: {}", executionId);
		}

		return ResponseEntity.badRequest().body("No JobExecution found for id: " + executionId);
	}

	@Tool(description = "Retrieves the status of a batch job based on the job execution ID.")
	public ResponseEntity<JobDetails> getJobStatus(@ToolParam(description = "The job execution ID") Long executionId)
			throws Exception {

		JobExecution execution = jobExplorer.getJobExecution(executionId);

		if (execution == null) {
			throw new NoSuchJobExecutionException("No JobExecution found for id: [" + executionId + "]");
		}
		return ResponseEntity.ok(new JobDetails(execution.getStatus().name(), execution.getId(),
				execution.getJobInstance().getInstanceId(), execution.getJobInstance().getJobName(),
				execution.getCreateTime(), execution.getEndTime(), execution.getLastUpdated()));
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
