# Spring AI MCP (Model Control Protocol) Demo

This project demonstrates the use of Spring AI with the Model Control Protocol (MCP) to create a weather service that generates creative poems based on current weather conditions.

## Project Overview

The project consists of 2 modules:

1. **MCP Server**: A Spring Boot application that provides weather data via MCP
2. **MCP Client**: A Spring Boot application that interacts with the MCP server and uses Ollama LLM

The application flow:
- The client sends a request to check the weather in some place
- The Ollama LLM prepares needed arguments to call MCP server
- MCP server is called
- The server retrieves weather data from OpenMeteo API
- The server requests the client to generate a poem about the weather (MCP Sampling feature demo)
- The client uses Ollama to generate the poem and sends it back to the server
- The server combines the weather data and poem into a final response
- The client receives response from server, calls LLM with the final prompt and displays the final response

### 1. Start Ollama using Docker Compose

From the project root directory, run:

```bash
docker-compose up -d
```

This will start the Ollama service on port 11431 and automatically pull the llama3.2 model.

### 2. Start the MCP Server

In a new terminal, navigate to the server module and start it:

```bash
cd spring-ai-mcp-server
mvn spring-boot:run
```

The server will start on port 8080.

### 3. Start the MCP Client

In another terminal, navigate to the client module and start it:

```bash
cd spring-ai-mcp-client
mvn spring-boot:run
```

### What Happens After Client Start

When the client starts:

1. It automatically sends a request to check the weather in Thessaloniki
2. You'll see log messages showing:
   - The initial request being sent
   - Progress notifications from the server (0%, 50%, 100%)
   - Log messages from the server
   - The sampling request for poem generation
   - The generated poem
   - The final response combining weather data and the poem

## Project Structure

- `docker-compose.yml`: Configuration for the Ollama service
- `spring-ai-mcp-server/`: The MCP server module
- `spring-ai-mcp-client/`: The MCP client module