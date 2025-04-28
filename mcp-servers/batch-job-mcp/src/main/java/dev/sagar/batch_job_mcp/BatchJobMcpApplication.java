package dev.sagar.batch_job_mcp;

import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import dev.sagar.batch_job_mcp.job.BatchJobService;

@SpringBootApplication
public class BatchJobMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchJobMcpApplication.class, args);
	}

	@Bean
	public List<ToolCallback> batchTools(BatchJobService batchJobService) {
		return List.of(ToolCallbacks.from(batchJobService));
	}

}
