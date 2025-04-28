# Finance Analysis MCP

A Spring Boot Model Context Protocol (MCP) server that provides AI-powered personal finance analysis tools. This project showcases the Spring AI MCP Server Boot Starter capabilities with STDIO transport implementation, combining natural language processing with SQL query generation for analyzing financial transactions stored in a PostgreSQL database.

## Prerequisites

* Java 21 or later
* PostgreSQL database
* Understanding of Spring Boot, Spring AI, and MCP concepts
* OpenAI API key for AI-powered financial analysis

## About Spring AI MCP Server Boot Starter

The project uses Spring AI's MCP Server Boot Starter to expose financial analysis operations as tools that can be consumed by AI assistants. It provides natural language processing of financial queries using OpenAI's models and executes SQL queries against a PostgreSQL database to analyze transaction data.

The project demonstrates how to implement and register MCP tools using Spring's dependency injection and auto-configuration:

```java
@Service
public class PersonalFinanceService {
    @Tool(description = "Generate SQL query based on user's financial question")
    public void generateSQL(String question) {
        // Implementation
    }

    @Tool(description = "Execute SQL query for financial analysis")
    public List<Map<String, Object>> executeSQLQuery(String sqlQuery) {
        // Implementation
    }
}
```

## Features

1. Natural Language Query Processing
   * Converts natural language questions into SQL queries
   * Understands financial context and terminology
   * Handles complex financial analysis requests
   * Supports both expense and income analysis

2. SQL Query Generation and Validation
   * Generates PostgreSQL-compatible queries
   * Ensures proper transaction type handling (DEBIT/CREDIT)
   * Validates string handling and case sensitivity
   * Provides query improvement feedback

3. Financial Data Analysis
   * Executes validated SQL queries
   * Formats results in human-readable format
   * Supports transaction categorization
   * Provides aggregated financial insights

## Available MCP Tools

The following tools are available through the PersonalFinanceService:

### generateSQL
Generates SQL queries based on natural language questions about personal finances. The tool follows specific rules for query generation including PostgreSQL compatibility, proper transaction type handling, and category validation.

### retrieveTableSchema
Retrieve the table schema to generate the SQL query. This provides the database structure necessary for query generation.

### retrieveListOfCategories
Retrieve the list of categories to generate the SQL query based on the user's question. This ensures that only valid categories are used in queries.

### validateSQLQuery
Validates the generated SQL query based on the user's question. This ensures queries are well-formed, compatible with PostgreSQL, and follow the system's rules.

### executeSQLQuery
Execute the generated SQL query based on the user's financial question against the database for data queries. Returns the query results in a structured format.

## Configuration

```properties
# Required STDIO Configuration
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.pattern.console=

# Server Configuration
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.stdio=true
spring.ai.mcp.server.name=personal-finance-analysis-mcp

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres

# OpenAI Configuration
spring.ai.openai.api-key=${OPENAI_API_KEY}
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

The server uses STDIO transport mode and is typically started automatically by the client. To use this financial analysis server with Claude Desktop, add the following configuration to your Claude Desktop settings:

```json
{
    "mcpServers": {
        "personal-finance-analysis": {
            "command": "java",
            "args": [
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-Dlogging.pattern.console=",
                "-Dlogging.file.name=log_path/personal-finance-analysis-mcp.log",
                "-jar",
                "/absolute/path/to/personal-finance-analysis-mcp-0.0.1-SNAPSHOT.jar"
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

The application uses the following PostgreSQL tables:

* `financial_transactions`: Main table containing both debit and credit transactions with details like amount, date, category, and transaction type
* `credit_card_payments`: Table specifically for tracking credit card payment transactions

If the `spring.profiles.active` in the application.properties file is set to 'local', these tables are automatically created using the schema defined in the application.

## Query Examples

The system can handle various types of financial queries such as:
* "What are my total expenses this month?"
* "Show me all income transactions from last week"
* "How much did I spend on groceries in March?"
* "Compare my spending for January - March."

## Resources

* [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
* [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-server-boot-starter-docs.html)
* [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
