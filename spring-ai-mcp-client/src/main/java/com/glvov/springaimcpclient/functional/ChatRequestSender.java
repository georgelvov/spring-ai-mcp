package com.glvov.springaimcpclient.functional;

import com.glvov.springaimcpclient.mcp.McpServerNotificationHandler;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.ChatModelCallAdvisor;
import org.springframework.ai.chat.client.advisor.DefaultAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Full flow of the request:
 * </br>
 * </br>
 * 1. {@link DefaultChatClient.DefaultCallResponseSpec#content}
 * </br>
 * It is a call at the end of the {@link #chatClient} fluent api call chain:
 * <pre><code>
 * chatClient
 *       .prompt(USER_PROMPT)
 *       .call()
 *       .content(); // <-------
 * </code></pre>
 * </br>
 * </br>
 * 2. {@link DefaultAroundAdvisorChain#nextCall}
 * </br>
 * The {@link DefaultAroundAdvisorChain} manages a {@code Deque<CallAdvisor>} of call advisors.
 * It retrieves the next advisor via {@code pop()} and executes {@link CallAdvisor#adviseCall}.
 * </br>
 * </br>
 * 3. {@link ChatModelCallAdvisor#adviseCall}
 * </br>
 * The first and the only one advisor we call is {@link ChatModelCallAdvisor#adviseCall}.
 * It is the only one because we don't configure any other advisors, and the {@code ChatModelCallAdvisor}
 * is default one and active by default. It calls inside the {@code chatModel.call()} method,
 * which is in our case is an instance of {@link OllamaChatModel}.
 * </br>
 * </br>
 * 4. {@link OllamaChatModel#call(Prompt)}
 * </br>
 * </br>
 * 4.1 Inside the {@code call} method we call a private method -
 * {@code OllamaChatModel.internalCall(prompt, previousChatResponse)}.
 * The {@code prompt} argument contains property {@code prompt.messages} with an info like:
 * </br>
 * <pre><code>
 * UserMessage(content="Check whether in Thessaloniki...")
 * </code></pre>
 * </br>
 * 4.2 Then the {@link OllamaApi.ChatRequest} is created for the Ollama LLM containing information
 * about available MCP Server tools, which we connected to this project, e.g. via application.yaml:
 * </br>
 * <pre><code>
 *   ChatRequest(
 *       messages = Message(
 *           content = "Check the weather in Thessaloniki right now and show the creative response!",
 *           tools = Tool(
 *               type = FUNCTION,
 *               Function(
 *                   name = getTemperature,
 *                   parameters = HashMap(
 *                       "properties = LinkedHashMap(
 *                           'latitude'='description of the latitude param',
 *                           'longitude'='description of the longitude param'
 *                       )"
 *                   )
 *               )
 *           )
 *       )
 *   )
 * </code></pre>
 * </br>
 * This request is sent to Ollama via {@link OllamaApi#chat} method sends this request to the `Ollama` LLM.
 * </br>
 * </br>
 * 4.3 Receiving the Ollama LLM Response.
 * Ollama’s role is simply to produce a JSON-like structure for our MCP server.
 * In this case, Ollama detects that a weather tool is available, and it requires two arguments: latitude and longitude.
 * From user’s prompt in the request it extracts the city name (e.g., Thessaloniki), resolves its coordinates,
 * and returns them in a structured format.
 * </br>
 * The response from Ollama looks as follows:
 * <pre><code>
 *      OllamaResponse: Message[
 *          role=ASSISTANT,
 *          content="",
 *          images=null,
 *          toolCalls=[
 *              ToolCall[
 *                  function=ToolCallFunction[
 *                      name=getTemperature,
 *                      arguments={latitude=40.6317, longitude=22.9353},
 *                      index=0
 *                  ]
 *              ]
 *          ],
 *          toolName=null,
 *          thinking=null
 *      ]
 * </code></pre>
 * </br>
 * </br>
 * 5. {@link DefaultToolCallingManager#executeToolCalls}
 * </br>
 * This method is invoked as:
 * <pre><code>
 *  toolCallingManager.executeToolCalls((Original prompt from USER) prompt, (OllamaResponse from 4.3) response)
 * </code></pre>
 * Internally, {@code executeToolCall} creates {@link ToolCallback} objects (tools) and
 * executes the {@link ToolCallback#call} method, in our case is the {@link SyncMcpToolCallback#call}.
 * </br>
 * </br>
 * 6. {@link SyncMcpToolCallback#call}
 * </br>
 * A {@link CallToolRequest} is built using the tool name, arguments, and metadata:
 * <pre><code>
 *    CallToolRequest.builder()
 *        .name(this.tool.name())    // e.g., getTemperature
 *        .arguments(arguments)     // e.g., latitude=40.6317, longitude=22.9353
 *        .meta(mcpMeta)
 *        .build();
 * </code></pre>
 * The {@code mcpClient.callTool(callToolRequest)} is executed.
 * </br>
 * </br>
 * 6.1 Receiving a call from the MCP Server to {@link McpServerNotificationHandler#samplingHandler}.
 * In this step, while executing {@code mcpClient.callTool(callToolRequest)} from the previous step,
 * the MCP Server requests additional information from the MCP client via
 * {@code McpServerNotificationHandler#samplingHandler}.
 * In our case, the client uses LLM to generate content for the MCP Server.
 * To do this, it invokes {@link DefaultChatClient} and follows the same sequence described in this javadoc,
 * starting from section 1 but excluding the tool-calling part, so {@link OllamaChatModel} returns
 * the result after the first LLM call (since no tool invocation is required).
 * </br>
 * </br>
 * 6.2 The sampling information is returned to the server and
 * the {@code mcpClient.callTool(callToolRequest)} execution is finished.
 * Content from the MCP Server is returned to {@code OllamaChatModel#internalCall} method.
 * </br>
 * </br>
 * 7. Recursive Call to the Ollama Model.
 * </br>
 * After receiving the tool execution result, recursive call to {@code OllamaChatModel#internalCall} occurs.
 * Comparing to the section 4.1, the {@code prompt.messages} structure now differs. It contains data from MCP Server:
 * <pre><code>
 *      UserMessage(content="Check whether in Thessaloniki...")
 *      AssistantMessage(
 *          toolCalls=[
 *              ToolCall[
 *                  id=, type=function, name=getTemperature,
 *                  arguments={"latitude":"40.6317","longitude":"22.9353"}
 *              ]
 *          ],
 *          ToolResponseMessage{
 *              responses=[
 *                  ToolResponse[
 *                      id=, name=getTemperature,
 *                      responseData=[
 *                          {"text":"Weather details: 10.40°C at (40.6317, 22.9353)"}
 *                      ]
 *                  ]
 *              ],
 *              messageType=TOOL,
 *              metadata={messageType=TOOL}
 *          }
 *      )
 * </code></pre>
 * The "Weather details: 10.40°C at (40.6317, 22.9353)" text is the response from the MCP server.
 * </br>
 * </br>
 * 8. {@link OllamaApi#chat}
 * </br>
 * Second and final call to Ollama LLM (first in section 4.2). The {@code OllamaResponse} now contains smth like:
 * <pre>
 * "Ah, sunshine seeker! I've got the scoop on Thessaloniki's current weather.
 *  Currently, it's a pleasant 10.40°C in this lovely Greek city."
 * </pre>
 * It is detected, that no other calls to the tools are required and the final chat result is returned to the caller.
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

    // Contains all registered MCP tools from connected servers
    private final ToolCallbackProvider mcpToolProvider;


    @Override
    public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
        log.info("Sending request to LLM...");
        log.info("User prompt:\n{}", USER_PROMPT);

        String response = chatClient
                .prompt(USER_PROMPT)
                .toolContext(Map.of("progressToken", "token-" + new Random().nextInt())) // unique progressToken
                .toolCallbacks(mcpToolProvider)
                .call()
                .content();

        log.info("---------------------");
        log.info("Final answer:\n{}", response);
        log.info("---------------------");
    }
}
