package com.glvov.springaimcpserver.tools.sampling;

import com.glvov.springaimcpserver.model.WeatherInfo;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PoemSamplingService {


    /**
     * Sampling Capability - the server can request the client's LLM to generate content.
     * <br>
     * The enhanced weather service now returns not just weather data, but a creative poem about the forecast,
     * demonstrating the bidirectional AI interaction pattern between MCP server and MCP client.
     */
    public String generatePoem(McpSyncServerExchange exchange, String progressToken, WeatherInfo weather) {
        if (exchange.getClientCapabilities().sampling() == null) {
            logSamplingAbsence(exchange);
            return null;
        }

        log.info("Starting poem sampling...");

        exchange.progressNotification(new ProgressNotification(progressToken, 0.5, 1.0, "Start sampling"));

        String userPrompt = """
                Weather forecast: %s°C
                Location: (%s, %s)
                Please write an epic Shakespearean-style poem about this weather.
                """.formatted(weather.temperature(), weather.latitude(), weather.longitude());

        var request = CreateMessageRequest.builder()
                .systemPrompt("You are a poet!")
                .messages(List.of(new SamplingMessage(Role.USER, new TextContent(userPrompt))))
                .build();

        String poem = ((TextContent) exchange.createMessage(request).content()).text();

        log.info("Poem is successfully generated on the MCP Client side. Size: {}", poem.length());

        return poem;
    }

    private void logSamplingAbsence(McpSyncServerExchange exchange) {
        log.warn("Sampling skipped, MCP Client doesn't provide sampling capability");

        exchange.loggingNotification(
                LoggingMessageNotification.builder()
                        .level(LoggingLevel.WARNING)
                        .data("MCP Server cannot perform sampling, because MCP Client doesn't provide sampling capability")
                        .build()
        );
    }
}
