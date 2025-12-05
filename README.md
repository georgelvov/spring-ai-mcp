# Spring AI MCP (Model Context Protocol) Demo

![MCP Schema](/images/mcp-schema.png)

This project demonstrates the use of Spring AI with the Model Context Protocol (MCP) to get current weather conditions for some place.

## Project Overview

The project consists of 2 modules:

1. **MCP Server**: A Spring Boot application that provides weather data via MCP
2. **MCP Client**: A Spring Boot application that interacts with the MCP server and uses Ollama LLM

The application flow:
- The client sends a request to check the weather in some place to LLM
- The Ollama LLM prepares needed arguments to call MCP server
- MCP server is called
- The server retrieves weather data from OpenMeteo API
- The server requests the client to generate a poem about the weather\*
- The client uses Ollama to generate the poem and sends it back to the server\*
- The server combines the weather data and poem into a final response
- The client receives response from server, calls LLM with the final prompt and displays the final response

\* MCP Sampling feature demo part

## Project Structure

- `docker-compose.yml`: Configuration for the Ollama service
- `spring-ai-mcp-server/`: The MCP server module
- `spring-ai-mcp-client/`: The MCP client module

## How to start

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

#### When the client starts:

1. It automatically sends a request to check the weather in Thessaloniki
2. You'll see log messages showing:
   - The initial request being sent
   - Progress notifications from the server (0%, 50%, 100%)
   - Log messages from the server
   - The sampling request for poem generation
   - The generated poem
   - The final response combining weather data and the poem

####  Full Flow Description ✨💡

For a detailed description of the full flow of the request-processing pipeline,  
refer to this [Javadoc](https://github.com/georgelvov/spring-ai-mcp/blob/main/spring-ai-mcp-client/src/main/java/com/glvov/springaimcpclient/functional/ChatRequestSender.java).

It provides a deep dive into the Spring AI module and explains how requests are handled across various components.

You can view the Javadoc for `ChatRequestSender` using your IDE's documentation viewer.  
Alternatively, you can generate the Javadoc for the project by running:

```bash
mvn javadoc:javadoc
```

## Troubleshooting Guide

### Common Issues and Solutions

#### 1. Error: `org.springframework.ai.retry.NonTransientAiException: HTTP 400 - {"error":"model is required"}`

**Description**:  
This error occurs when a required LLM model is not properly loaded or configured in the Ollama container. Without the model, the client cannot process requests, leading to this error.

**Solution**:  
Restart the Ollama container to ensure the required model is loaded properly.  
Run the following command in the terminal:

```bash
docker-compose restart ollama
```

Alternatively, bring down and restart all services to fully refresh the environment:

```bash
docker-compose down
docker-compose up -d
```

After restarting, verify that the Ollama container is running and accessible on port `11431` with the correct model loaded.
