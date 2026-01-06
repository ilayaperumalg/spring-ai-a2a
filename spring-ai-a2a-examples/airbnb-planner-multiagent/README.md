# Airbnb Planner Multi-Agent Example

This example demonstrates a **multi-agent travel planning system** using Spring AI's A2A (Agent-to-Agent) protocol. It showcases how to build distributed agent systems where specialized agents collaborate to handle complex user requests.

## Architecture

The system consists of three Spring Boot applications that communicate via the A2A protocol:

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Request                             │
│              (e.g., "Find a room in LA, June 20-25,             │
│                       what's the weather?")                      │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
                   ┌──────────────────┐
                   │   Host Agent     │  Port: 8080
                   │  (Orchestrator)  │
                   │                  │
                   │ • Routes requests│
                   │ • Combines       │
                   │   responses      │
                   └──────┬───────┬───┘
                          │       │
              ┌───────────┘       └───────────┐
              │                               │
              │ A2A Protocol                  │ A2A Protocol
              │ (HTTP/JSON-RPC)               │ (HTTP/JSON-RPC)
              ▼                               ▼
    ┌──────────────────┐          ┌──────────────────┐
    │  Weather Agent   │          │  Airbnb Agent    │
    │   Port: 10001    │          │   Port: 10002    │
    │                  │          │                  │
    │ • Provides       │          │ • Searches       │
    │   weather info   │          │   accommodations │
    │ • Uses ChatClient│          │ • Uses ChatClient│
    └──────────────────┘          └──────────────────┘
```

### Agents

1. **Weather Agent** (Port 10001)
   - Provides weather information for locations
   - Skills: Weather queries, forecasts, temperature data
   - Implementation: `WeatherAgentExecutor` extending `DefaultSpringAIAgentExecutor`

2. **Airbnb Agent** (Port 10002)
   - Searches and recommends accommodations
   - Skills: Accommodation search, booking recommendations
   - Implementation: `AirbnbAgentExecutor` extending `DefaultSpringAIAgentExecutor`

3. **Host Agent** (Port 8080)
   - Orchestrates requests to specialized agents
   - Routes queries based on content analysis
   - Combines responses from multiple agents
   - Implementation: Uses `RemoteAgentRegistry` to manage remote agents

## Key Concepts Demonstrated

### 1. DefaultSpringAIAgentExecutor

The Weather and Airbnb agents demonstrate extending `DefaultSpringAIAgentExecutor` to implement the `AgentExecutorLifecycle` interface:

```java
@Bean
public AgentExecutor agentExecutor(@Autowired(required = false) ChatClient chatClient) {
    return new WeatherAgentExecutor(chatClient);
}
```

This approach:
- Extends `DefaultSpringAIAgentExecutor` which implements the A2A Java SDK `AgentExecutor` interface
- Implements simplified lifecycle methods from `AgentExecutorLifecycle`
- Integrates Spring AI `ChatClient` for LLM capabilities
- Provides full control over agent behavior
- Follows the A2A SDK patterns while simplifying implementation

### 2. Agent Card Configuration

Each agent programmatically creates its `AgentCard` describing capabilities:

```java
@Bean
public AgentCard agentCard() {
    return AgentCard.builder()
        .name("Weather Agent")
        .description("Provides weather information for any location")
        .version("1.0.0")
        .protocolVersion("0.1.0")
        .skills(List.of(
            AgentSkill.builder()
                .id("weather-info")
                .name("Get Weather Information")
                .description("Provides current weather conditions...")
                .tags(List.of("weather", "forecast"))
                .build()
        ))
        .build();
}
```

### 3. A2A Client Usage

The Host Agent demonstrates connecting to remote agents using the A2A Agent architecture:

```java
// Direct DefaultA2AAgentClient creation
A2AAgent weatherAgent = DefaultA2AAgentClient.builder()
    .agentUrl("http://localhost:10001/a2a")
    .build();

// Use the agent directly
A2AResponse response = weatherAgent.sendMessage(A2ARequest.of("Hello, agent!"));
```

### 4. New A2A Agent Architecture (Spring AI 2.0+)

Spring AI 2.0 introduces a unified agent architecture:

**A2AAgent Interface:**
```java
public interface A2AAgent {
    AgentCard getAgentCard();
    A2AResponse sendMessage(A2ARequest request);
    Flux<A2AResponse> sendMessageStreaming(A2ARequest request);
    boolean supportsStreaming();
}
```

**Key Benefits:**
- **Unified abstraction**: Both local (`A2AAgentServer`) and remote (`A2AAgentClient`) agents implement the same interface
- **Direct usage**: Agents can be used directly: `agent.sendMessage(request)`
- **Streaming support**: Both sync and async streaming via `sendMessageStreaming()`
- **Flexible**: Easy to mock for testing, extend for custom transports

**Example Usage:**
```java
// Direct agent usage
A2AAgent agent = DefaultA2AAgentClient.builder().agentUrl("http://localhost:10001/a2a").build();
A2AResponse response = agent.sendMessage(A2ARequest.of("What's the weather?"));

// Streaming usage
Flux<A2AResponse> stream = agent.sendMessageStreaming(A2ARequest.of("Tell me a story"));
```

### 5. Multi-Agent Orchestration

The Host Agent intelligently routes requests to appropriate agents and combines responses:

```java
private String determineAgentType(String userMessage) {
    boolean needsWeather = message.contains("weather");
    boolean needsAirbnb = message.contains("room") || message.contains("accommodation");

    if (needsWeather && needsAirbnb) {
        return "both";  // Query both agents
    }
    // ... route to specific agent
}
```

## Prerequisites

- **Java 25+** (required to run Maven build)
- **Maven** (or use the included `./mvnw` wrapper)
- **OpenAI API Key** (set as `OPENAI_API_KEY` environment variable)

## Setup

### 1. Set Environment Variable

```bash
export OPENAI_API_KEY=your_openai_api_key_here
```

### 2. Build the Project

From the `spring-ai` root directory:

```bash
./mvnw clean install -DskipTests -pl spring-ai-a2a/spring-ai-a2a-examples/airbnb-planner-multiagent -am
```

Or build just the examples:

```bash
cd spring-ai-a2a/spring-ai-a2a-examples/airbnb-planner-multiagent
mvn clean package
```

## Running the System

You need to start **all three agents** in separate terminal windows.

### Terminal 1: Weather Agent

```bash
cd spring-ai-a2a/spring-ai-a2a-examples/airbnb-planner-multiagent/weather-agent-server
mvn spring-boot:run
```

Expected output:
```
Started WeatherAgentApplication in X.XXX seconds (process running on 10001)
```

### Terminal 2: Airbnb Agent

```bash
cd spring-ai-a2a/spring-ai-a2a-examples/airbnb-planner-multiagent/airbnb-agent-server
mvn spring-boot:run
```

Expected output:
```
Started AirbnbAgentApplication in X.XXX seconds (process running on 10002)
```

### Terminal 3: Host Agent

```bash
cd spring-ai-a2a/spring-ai-a2a-examples/airbnb-planner-multiagent/host-agent-server
mvn spring-boot:run
```

Expected output:
```
Connecting to Weather Agent at: http://localhost:10001/a2a
✓ Weather Agent registered successfully
Connecting to Airbnb Agent at: http://localhost:10002/a2a
✓ Airbnb Agent registered successfully
Host Agent initialization complete!
API available at: http://localhost:8080/api/query
```

## Testing the System

### Using curl

**Weather Query:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the weather like in Los Angeles, CA?"}'
```

**Accommodation Query:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"message": "Find me a room in Los Angeles, CA for June 20-25, 2025 for two adults"}'
```

**Combined Query:**
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"message": "I need accommodation in LA for June 20-25 for 2 people. Also tell me about the weather."}'
```

**Health Check:**
```bash
curl http://localhost:8080/api/health
```

### Expected Response Format

```json
{
  "message": "your query here",
  "response": "## Weather for Los Angeles, CA\n**Date:** June 20-25, 2025\n..."
}
```

## Configuration

### Customizing Agent URLs

Edit `host-agent-server/src/main/resources/application.properties`:

```properties
weather.agent.url=http://localhost:10001/a2a
airbnb.agent.url=http://localhost:10002/a2a
```

### Customizing Ports

Edit each agent's `application.properties`:

**Weather Agent:**
```properties
server.port=10001
```

**Airbnb Agent:**
```properties
server.port=10002
```

**Host Agent:**
```properties
server.port=8080
```

## Project Structure

```
airbnb-planner-multiagent/
├── pom.xml                          # Parent POM
├── README.md                        # This file
├── weather-agent-server/
│   ├── pom.xml
│   └── src/main/java/.../weather/
│       ├── WeatherAgentExecutor.java      # A2A agent implementation
│       └── WeatherAgentApplication.java   # Spring Boot app
├── airbnb-agent-server/
│   ├── pom.xml
│   └── src/main/java/.../airbnb/
│       ├── AirbnbAgentExecutor.java       # A2A agent implementation
│       └── AirbnbAgentApplication.java    # Spring Boot app
└── host-agent-server/
    ├── pom.xml
    └── src/main/java/.../host/
        ├── RemoteAgentRegistry.java       # Manages remote agent connections
        ├── HostAgentOrchestrator.java     # Routes and combines requests
        ├── HostAgentController.java       # REST API
        └── HostAgentApplication.java      # Spring Boot app
```

## Troubleshooting

### Agent Registration Fails

**Problem:** Host agent reports "Failed to register Weather Agent" or "Failed to register Airbnb Agent"

**Solution:**
1. Ensure the specialized agents are running before starting the host agent
2. Check that the URLs in `application.properties` are correct
3. Verify no firewall is blocking the ports

### Connection Refused

**Problem:** `Connection refused` errors when calling the host agent

**Solution:**
1. Confirm all three agents are running: `ps aux | grep java`
2. Check each agent's logs for startup errors
3. Verify ports 8080, 10001, and 10002 are not in use by other applications

### OpenAI API Errors

**Problem:** "OpenAI API key not found" or rate limit errors

**Solution:**
1. Verify `OPENAI_API_KEY` environment variable is set: `echo $OPENAI_API_KEY`
2. Restart the agents after setting the environment variable
3. Check your OpenAI account for rate limits and billing status

## Comparison to Python Example

This Spring AI implementation follows the same architecture as the [Python A2A multi-agent sample](https://github.com/a2aproject/a2a-samples/tree/main/samples/python/agents/airbnb_planner_multiagent):

| Aspect | Python (Google ADK) | Spring AI |
|--------|---------------------|-----------|
| **Agent Implementation** | Implements `AgentExecutor` from a2a-python SDK | Extends `DefaultSpringAIAgentExecutor` implementing `AgentExecutorLifecycle` |
| **LLM Integration** | Uses Google Gemini via `LiteLlm` | Uses OpenAI via Spring AI `ChatClient` |
| **Agent Abstraction** | N/A | Uses `A2AAgent` interface for unified local/remote agent access |
| **Remote Communication** | A2A client with async/await | `DefaultA2AAgentClient` implementing `A2AAgent` interface |
| **Agent Registry** | Manual tracking | Direct `DefaultA2AAgentClient` usage |
| **Orchestration** | Python routing agent with state management | Java `HostAgentOrchestrator` with intelligent routing |
| **Configuration** | Environment variables + .env files | Spring Boot application.properties + environment variables |

## Implementation Approach

This example uses the **lifecycle-based approach** with `DefaultSpringAIAgentExecutor`:

| Feature | Lifecycle-Based (This Example) |
|---------|-------------------------------|
| **Agent Definition** | Manual `AgentCard` bean |
| **Execution Method** | Extend `DefaultSpringAIAgentExecutor` and implement `onExecute()` |
| **Flexibility** | Full control over agent behavior and lifecycle |
| **SDK Alignment** | Directly implements A2A SDK `AgentExecutor` interface |
| **Use Case** | All agent types, production-ready implementations |

## Next Steps

- **Add Authentication:** Secure the host agent API with Spring Security
- **Implement Caching:** Cache agent responses for better performance
- **Add Streaming:** Support streaming responses from agents
- **Error Recovery:** Implement retry logic and circuit breakers
- **Observability:** Add distributed tracing with Micrometer
- **Real APIs:** Replace simulated data with real weather and Airbnb APIs

## References

- [A2A Protocol Specification](https://a2a-protocol.org)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [A2A Python Sample (Original)](https://github.com/a2aproject/a2a-samples/tree/main/samples/python/agents/airbnb_planner_multiagent)
- [A2A Java SDK](https://github.com/a2aproject/a2a-java-sdk)

## License

Copyright 2025-2026 the original author or authors.

Licensed under the Apache License, Version 2.0.
