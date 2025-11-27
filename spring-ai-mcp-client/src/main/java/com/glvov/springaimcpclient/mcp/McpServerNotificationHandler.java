package com.glvov.springaimcpclient.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class McpServerNotificationHandler {

    private final ChatClient chatClient;

    /**
     * Handles log notifications from MCP Server
     */
    @McpLogging(clients = "my-weather-server")
    public void loggingHandler(LoggingMessageNotification loggingMessage) {
        log.info("MCP Server log: [{}] {}", loggingMessage.level(), loggingMessage.data());
    }

    /**
     * Handles progress notifications from MCP Server
     */
    @McpProgress(clients = "my-weather-server")
    public void progressHandler(ProgressNotification progressNotification) {
        log.info("MCP Server Progress notification received: [{}] progress: {} | message: {}",
                progressNotification.progressToken(),
                (int)(progressNotification.progress() * 100) + "% done",
                progressNotification.message()
        );
    }

    /**
     * Sampling Capability
     * Handles MCP server requests to the MCP client's LLM to generate content.
     * <br>
     * It demonstrates the bidirectional AI interaction pattern between MCP server and MCP client.
     */
    @McpSampling(clients = "my-weather-server")
    public McpSchema.CreateMessageResult samplingHandler(CreateMessageRequest llmRequest) {
        log.info("MCP Server sampling request: {}", llmRequest);

        String systemPrompt = llmRequest.systemPrompt();
        TextContent userPrompt = (TextContent) llmRequest.messages().getFirst().content();

        String llmResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt.text())
                .options(ChatOptions.builder().maxTokens(100).build())
                .call()
                .content();

        log.info("******************************");
        log.info("Sampling LLM response:\n{}", llmResponse);
        log.info("******************************");

        return McpSchema.CreateMessageResult
                .builder()
                .content(new TextContent(llmResponse))
                .build();
    }
}
