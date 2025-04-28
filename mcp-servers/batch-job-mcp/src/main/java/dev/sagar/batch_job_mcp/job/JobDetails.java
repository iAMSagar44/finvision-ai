package dev.sagar.batch_job_mcp.job;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

record JobDetails(String status, Long jobExecutionId, @JsonInclude(Include.NON_NULL) Long jobInstanceId, String jobName,
		LocalDateTime startTime, @JsonInclude(Include.NON_NULL) LocalDateTime endTime,
		@JsonInclude(Include.NON_NULL) LocalDateTime lastUpdated) {

	public JobDetails(String status, Long jobExecutionId, Long jobInstanceId, String jobName, LocalDateTime startTime) {
		this(status, jobExecutionId, jobInstanceId, jobName, startTime, null, null);
	}
}
