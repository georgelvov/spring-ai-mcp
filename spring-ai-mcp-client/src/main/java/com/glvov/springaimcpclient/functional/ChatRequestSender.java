package com.glvov.springaimcpclient.functional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * 1. ToolCallbackProvider - contains all registered MCP tools from connected servers
 * 2. toolContext to pass a unique progressToken to MCP tools annotated with @McpProgressToken parameter
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatRequestSender implements ApplicationListener<ApplicationStartedEvent> {

    private static final String USER_PROMPT = """
            Check the weather in Thessaloniki right now and show the creative response!
            Please incorporate all creative responses from all LLM providers.
            """;

    private final ChatClient chatClient;

    // 1
    private final ToolCallbackProvider mcpToolProvider;


    @Override
    public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
        log.info("Sending request to LLM...");
        log.info("User prompt:\n{}", USER_PROMPT);

        String response = chatClient
                .prompt(USER_PROMPT)
                .toolContext(Map.of("progressToken", "token-" + new Random().nextInt())) // 2
                .toolCallbacks(mcpToolProvider)
                .call()
                .content();

        log.info("---------------------");
        log.info("Final answer:\n{}", response);
        log.info("---------------------");
    }
}
