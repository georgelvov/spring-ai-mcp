package com.glvov.springaimcpserver.tools;

import com.glvov.springaimcpserver.functional.OpenMeteoGateway;
import com.glvov.springaimcpserver.model.WeatherInfo;
import com.glvov.springaimcpserver.tools.sampling.PoemSamplingService;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpProgressToken;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {

    private final OpenMeteoGateway openMeteoGateway;
    private final PoemSamplingService poemSamplingService;


    /**
     * MCP tool for retrieving the current temperature for a specific geographic location.
     * <br>
     * Additionally, it generates a contextualized poem related to the weather conditions and
     * communicates progress updates back to the client using MCP notifications.

     * @param exchange      provides access to server-client communication capabilities.
     *                      It allows the server to send notifications and make requests back to the client
     * @param progressToken enables progress tracking. The client provides this token,
     *                      and the server uses it to send progress updates.
     */
    @McpTool(description = "Get the temperature (in celsius) for a specific location")
    public String getTemperature(McpSyncServerExchange exchange, // (1)
                                 @McpToolParam(description = "The location latitude")
                                 double latitude,
                                 @McpToolParam(description = "The location longitude")
                                 double longitude,
                                 @McpProgressToken
                                 String progressToken) { // (2)

        log.info("getTemperature called with latitude={}, longitude={}, progressToken={}",
                latitude, longitude, progressToken);

        exchange.loggingNotification(
                LoggingMessageNotification.builder()
                        .level(LoggingLevel.INFO)
                        .data("getTemperature called with latitude=%s, longitude=%s".formatted(latitude, longitude))
                        .meta(Map.of()) // non-null meta as a workaround for bug
                        .build()
        );

        exchange.progressNotification(
                new ProgressNotification(progressToken, 0.0, 1.0, "Start getting temperature from open meteo")
        );

        WeatherInfo weather = openMeteoGateway.getWeather(latitude, longitude);

        log.info("Weather info: {}", weather);

        String poem = poemSamplingService.generatePoem(exchange, progressToken, weather);

        exchange.progressNotification(new ProgressNotification(progressToken, 1.0, 1.0, "Task completed"));

        String finalResponse = formatFinalResponse(poem, weather);

        log.info("------------------------------------");
        log.info("Final response:\n{}", finalResponse);
        log.info("------------------------------------");

        return finalResponse;
    }

    private String formatFinalResponse(String poem, WeatherInfo weather) {
        String poemContext = StringUtils.hasText(poem)
                ? "Weather Poem:\n%s".formatted(poem)
                : "";

        return """
                %s
                Weather details: %.2f°C at (%.4f, %.4f)
                """.formatted(poemContext, weather.temperature(), weather.latitude(), weather.longitude());
    }
}