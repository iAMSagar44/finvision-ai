# Spring AI MCP Batch Job Server

A Spring Boot Model Context Protocol (MCP) server that provides batch processing tools for financial transactions. This project showcases the Spring AI MCP Server Boot Starter capabilities with STDIO transport implementation, combined with Spring Batch for processing financial data.

## Prerequisites

* Java 21 or later
* PostgreSQL database
* Understanding of Spring Boot, Spring Batch, Spring AI and MCP concepts
* OpenAI API key for AI-powered transaction categorization

## About Spring AI MCP Server Boot Starter

The project uses Spring AI's MCP Server Boot Starter to expose batch processing operations as tools that can be consumed by AI assistants. It combines this with Spring Batch to process financial transaction data, categorize transactions using AI, and store them in a PostgreSQL database.

## Tool Implementation

The project demonstrates how to implement and register MCP tools using Spring's dependency injection and auto-configuration:

```java
@Service
public class BatchJobService {
    @Tool(description = "Triggers a batch job based on the provided file name.")
    public ResponseEntity<JobDetails> startJob(String fileName) {
        // Implementation
    }

    @Tool(description = "Restarts a failed job based on the job execution ID.")
    public ResponseEntity<?> restartJob(Long executionId) {
        // Implementation
    }

    @Tool(description = "Retrieves the status of a batch job based on the job execution ID.")
    public ResponseEntity<JobDetails> getJobStatus(Long executionId) {
        // Implementation
    }
}
```

## Available MCP Tools

### startJob
   - Triggers a batch job to process a financial transactions file
   - Requires the file name
   - Reads CSV files from the user's Downloads directory
   - Returns job execution details including ID and status

### restartJob
   - Restarts a failed job execution
   - Requires the job execution ID of the failed job
   - Returns the new job execution details

### getJobStatus
   - Retrieves the current status of a job
   - Requires the job execution ID
   - Provides detailed execution information including start/end times
   - Useful for monitoring job progress

## Processing Pipeline

The batch job processes financial transactions through multiple steps:

1. File Movement Step
   - Moves the input file to a temporary location
   - Ensures file safety during processing

2. Transaction Processing Step
   - Reads CSV files with transaction data
   - Filters and categorizes credit/debit transactions
   - Uses AI to categorize transactions by type
   - Writes processed data to PostgreSQL database

3. Cleanup Step
   - Removes temporary files after processing
   - Ensures clean workspace for next execution

## Configuration

### Application Properties

```properties
# Required STDIO Configuration
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.pattern.console=

# Server Configuration
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.stdio=true
spring.ai.mcp.server.name=batch-job-mcp

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.batch.jdbc.initialize-schema=always

# OpenAI Configuration
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o
```

## Building and Running

Ensure you have a PostgreSQL database up and running. You can set one up using Docker.

```bash
docker run --name finvision-ai -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:latest
```

The server uses STDIO transport mode and is typically started automatically by the client. To build the server jar:

```bash
./mvnw clean install
```

## Client Integration

To use this batch processing server with Claude Desktop, add the following configuration to your Claude Desktop settings:

```json
{
    "mcpServers": {
        "spring-ai-batch-mcp": {
            "command": "java",
            "args": [
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-Dlogging.pattern.console=",
                "-Dlogging.file.name=<log_path/batch-job-mcp-0.0.1-SNAPSHOT.log>",
                "-jar",
                "/absolute/path/to/batch-job-mcp-0.0.1-SNAPSHOT.jar"
            ],
            "env": {
                "OPENAI_API_KEY": "<YOUR_API_KEY>"
            }
        }
    }
}
```

Replace `/absolute/path/to/` with the actual path to your built jar file.

Replace `log_path` with the actual path where you want the logs to be generated.
## Database Schema

The application requires the following PostgreSQL tables:

- `financial_transactions`: Main transactions table
- `credit_transactions`: Credit transactions
- `credit_card_payments`: Credit card payment transactions
- `categories`: List of transaction categories

If the `spring.profiles.active` in the application.properties file is set to 'local', then these tables are automatically created.

The Spring Batch tables are automatically created when `spring.batch.jdbc.initialize-schema=always`.

## Additional Resources

* [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
* [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-server-boot-starter-docs.html)
* [Spring Batch Documentation](https://docs.spring.io/spring-batch/reference/)
