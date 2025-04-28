# Personal Finance Manager

An AI-powered personal finance management system designed to help users gain deep insights into their financial health and spending patterns. This intelligent financial assistant analyzes banking transactions and provides detailed spending analysis, making it particularly valuable for major financial decisions. For example, when preparing for a home purchase, the system can help track savings patterns, analyze debt-to-income ratios, and categorize expenses - essential metrics that lenders evaluate during the mortgage application process. The application leverages advanced AI capabilities to provide actionable financial insights and spending recommendations.

## Features

- **Bank Statement Analysis**: Upload and automatically categorize bank transactions using AI
- **Intelligent Financial Assistant**: Natural language interface for querying financial data
- **Batch Processing**: AI-assisted batch job management for large transaction processing
- **Transaction Categorization**: Smart categorization of expenses using AI
- **Real-time Analysis**: Interactive financial data analysis and visualization

## Technology Stack

- **Spring Boot**: Core application framework
- **Spring AI**: AI integration framework for implementing agentic patterns
- **Vaadin**: Modern web UI framework
- **PostgreSQL**: Primary database
- **OpenAI Integration**: GPT-4o and o3-mini for transaction analysis
- **Google Gemini Integration**: Additional AI capabilities (for OCR) through OpenAI-compatible API

## Agentic Patterns Implementation

This application implements the following AI Agents:

### Financial Service Agents

#### frontDeskService (gpt-4.1 model)
Acts as an initial conversational interface that acknowledges user queries about their financial data in a natural, friendly way. It doesn't perform the actual analysis but rather confirms the understanding of the user's financial question before it gets processed by other components.

#### analyseUserQuestion (o4-mini model)
An intelligent agent that enhances query processing by:
- Analyzing and rewriting user questions for optimal database querying
- Managing conversation memory to avoid redundant processing
- Checking if answers already exist in conversation context
- Providing contextually aware responses using o4-mini model
- Maintaining conversation history for better context awareness

#### generateQuery (gpt-4.1 model)
Generates SQL queries based on natural language questions about personal finances. The tool follows specific rules for query generation including PostgreSQL compatibility, proper transaction type handling, and category validation.

#### evaluateQuery (o4-mini model)
Validates the generated SQL query based on the user's question. This ensures queries are well-formed, compatible with PostgreSQL, and follow the system's rules.

#### getAggregatedData
Execute the generated SQL query based on the user's financial question against the database for data queries. Returns the query results in a structured format.

### View Layer Orchestration
#### CashFlowView Workflow
The CashFlowView implements a sophisticated workflow that orchestrates multiple AI agents to process financial queries:

1. **Initial Query Reception**: Uses the frontDeskService agent to interpret and acknowledge the user's financial question
2. **Query Analysis and Context Check**:
   - Delegates to the analyseUserQuestion agent to analyze and potentially rewrite the query
   - Checks conversation memory for existing answers to similar questions
   - If an answer exists in memory, returns it directly without additional processing
   - If no answer exists, proceeds with query generation
3. **Query Generation and Validation Loop**:
   - Delegates to the generateQuery agent to convert the analyzed/rewritten query into SQL
   - Employs the evaluateQuery agent to ensure query correctness and safety
   - If validation fails, automatically triggers the generateQuery agent again with the validation feedback
   - This loop continues up to 3 times until a valid query is generated or all retries are exhausted
4. **Data Retrieval**: Utilizes the getAggregatedData agent to execute the validated query (only after successful validation)
5. **Response Formatting**: Processes the retrieved data into a user-friendly format

This orchestration pattern ensures a seamless flow from user input to meaningful financial insights, with each agent specializing in its specific task while maintaining a cohesive interaction model. The automatic retry mechanism for query generation demonstrates the system's self-healing capabilities, as the LLM intelligently refines queries based on validation feedback without requiring user intervention. The addition of conversation memory through queryAnalyserService improves response times and reduces redundant processing by reusing previously computed answers when appropriate.

#### bankStatementAnalyzer (gemini-2.0-flash model)
An intelligent agent that processes uploaded credit card statements (PDF format) to extract and categorize financial transactions. It uses multimodal capabilities of AI to:
- Parse transaction details from bank statements
- Automatically categorize each transaction based on its description
- Filter out specific transactions like opening/closing balances
- Validate transaction amounts and dates
- Map transactions to predefined categories
- Uses Structured Output capability to return the response in a structured format

### Batch Job Management Agents

#### getJobStatus
Retrieves detailed status information for batch jobs using their execution ID. This agent monitors and reports on the progress and state of running batch jobs.

#### triggerBatchJob
Initiates new batch processing jobs for financial data files. This agent handles the orchestration of starting new batch processes for transaction analysis.
The application checks if the file exists in the user's `Downloads` folder before proceeding. If the file is not found, an error response is returned.
Requires users to provide a filename from their `Downloads` folder. Currently only `csv` files are supported.

#### restartJob
Manages the recovery of failed batch jobs by restarting using the job execution id.

See the examples section below for a few example queries.

## Getting Started

### Prerequisites
- Java 21
- PostgreSQL database
- OpenAI API key
- Google Gemini API key (used in 'Bank Statement View' to extract data from bank statements)

### Configuration

Set the following environment variables:
```properties
OPENAI_API_KEY=your_openai_api_key
GEMINI_API_KEY=your_gemini_api_key
```
## Database Schema

The application uses the following PostgreSQL table:

* `financial_transactions`: Main table containing both debit and credit transactions with details like amount, date, category, and transaction type

If the `spring.profiles.active` in the application.properties file is set to 'local', the tables are automatically created using the schema defined in the application.

### Running the Application

1. Clone the repository and change directory to this project folder
2. Set up PostgreSQL:
   - **Using Docker**:
```bash
docker run --name finvision-ai -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:latest
```
3. Configure the active profile (local or dev) in `application.properties`
4. Run using Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
5. Access the application at `http://localhost:8080`

## Query Examples

The application can handle various types of financial queries such as:
* "What are my total expenses this month?"
* "Show me all income transactions from last week"
* "How much did I spend on groceries in March?"
* "Compare my spending for January - March."

Batch job related queries:
* "Trigger a new job. The file name is Statement-Jan2024ToMarch2025.csv"
* "Find the status of the job 41"
* "Can you restart the job 41"
