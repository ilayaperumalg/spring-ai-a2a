# Spring AI A2A

Spring AI integration for the Agent-to-Agent (A2A) Protocol, enabling composable AI agents that communicate via the A2A protocol specification.

## Overview

Spring AI A2A provides Spring Boot integration for building AI agents that can communicate with each other using the [A2A Protocol](https://a2a.anthropic.com/). The project uses the [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk) directly for maximum compatibility and minimal abstraction overhead.

### Key Features

- **A2A Protocol Support**: Full implementation of the A2A protocol for agent-to-agent communication
- **Spring Boot Integration**: Auto-configuration and Spring Boot starters for rapid development
- **Direct A2A SDK Usage**: Built directly on the A2A Java SDK without intermediate abstraction layers
- **JSON-RPC Server**: Exposes agents via HTTP/JSON-RPC endpoints
- **Client Library**: Call remote A2A agents from your Spring applications
- **Spring AI Integration**: Seamless integration with Spring AI ChatClient for LLM interactions

## Architecture

### Core Components

The project consists of several modules:

```
spring-ai-a2a/
├── spring-ai-a2a-core/           # Core abstractions and A2A client
├── spring-ai-a2a-server/         # A2A server implementation and agent execution
├── spring-ai-a2a-client/         # Client for calling remote A2A agents
├── spring-boot-starter-spring-ai-a2a/  # Spring Boot auto-configuration
├── spring-ai-a2a-examples/       # Example applications
└── spring-ai-a2a-integration-tests/  # Integration tests
```

### Agent Execution Model

Agents implement the `A2AExecutor` interface, which extends the A2A SDK's `AgentExecutor`:

```java
public interface A2AExecutor extends AgentExecutor {
    ChatClient getChatClient();
    String getSystemPrompt();

    Message executeSynchronous(Message request);
    Flux<Message> executeStreaming(Message request);
}
```

### Message Types

The project uses A2A SDK message types directly:

- `io.a2a.spec.Message` - Messages between agents
- `io.a2a.spec.Part` - Message parts (text, data, etc.)
- `io.a2a.spec.TextPart` - Text message parts
- `io.a2a.spec.AgentCard` - Agent metadata and capabilities

## Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.8+
- OpenAI API key (for examples)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/your-org/spring-ai-a2a.git
cd spring-ai-a2a

# Build all modules
mvn clean install -DskipTests
```

### Creating an A2A Agent

1. **Add the Spring Boot starter dependency:**

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-boot-starter-spring-ai-a2a</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

2. **Create an agent executor:**

```java
@Component
public class MyAgent extends DefaultA2AExecutor {

    public MyAgent(ChatModel chatModel) {
        super(ChatClient.builder(chatModel).build());
    }

    @Override
    public String getSystemPrompt() {
        return "You are a helpful assistant that can answer questions.";
    }
}
```

3. **Configure agent metadata:**

```java
@Bean
public AgentCard agentCard() {
    return AgentCard.builder()
        .name("My Agent")
        .description("A helpful AI assistant")
        .version("1.0.0")
        .protocolVersion("0.1.0")
        .capabilities(AgentCapabilities.builder()
            .streaming(true)
            .build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .supportedInterfaces(List.of(
            new AgentInterface("JSONRPC", "http://localhost:8080/a2a")
        ))
        .build();
}
```

4. **Configure the server (application.yml):**

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

5. **Run your agent:**

```bash
mvn spring-boot:run
```

Your agent is now available at `http://localhost:8080/a2a`

### Calling an Agent

Use the A2A client to call remote agents:

```java
@Component
public class MyService {

    private final A2AClient weatherAgent;

    public MyService(@Value("${weather.agent.url}") String weatherAgentUrl) {
        this.weatherAgent = DefaultA2AClient.builder()
            .agentUrl(weatherAgentUrl)
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    public String getWeather(String location) {
        Message request = Message.builder()
            .role(Message.Role.USER)
            .parts(List.of(new TextPart("What's the weather in " + location + "?")))
            .build();

        Message response = weatherAgent.sendMessage(request);
        return MessageUtils.extractText(response.parts());
    }
}
```

## Examples

The project includes several example applications demonstrating different A2A patterns:

### Airbnb Planner Multi-Agent Example

Location: `spring-ai-a2a-examples/airbnb-planner-multiagent/`

Demonstrates a multi-agent system where a travel planning agent delegates to specialized agents:

- **Travel Planner Agent** (port 8080): Main orchestration agent
- **Accommodation Agent** (port 10002): Provides hotel recommendations

**Running the example:**

```bash
# Terminal 1: Start accommodation agent
cd spring-ai-a2a-examples/airbnb-planner-multiagent/accommodation-agent
mvn spring-boot:run

# Terminal 2: Start travel planner agent
cd spring-ai-a2a-examples/airbnb-planner-multiagent/travel-planner-agent
mvn spring-boot:run

# Terminal 3: Test the agent
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "sendMessage",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"text": "Plan a 3-day trip to Tokyo"}]
      }
    },
    "id": 1
  }'
```

## Configuration

### Agent Server Configuration

Configure the A2A server in `application.yml`:

```yaml
spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a              # Default: /a2a
      agent:
        name: "My Agent"
        description: "Agent description"
        version: "1.0.0"
        protocol-version: "0.1.0"
        capabilities:
          streaming: true
          push-notifications: false
          state-transition-history: false
        default-input-modes:
          - text
        default-output-modes:
          - text
```

### ChatModel Configuration

The project supports any Spring AI ChatModel. Example with OpenAI:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
```

## API Reference

### A2AExecutor

Main interface for implementing agents:

```java
public interface A2AExecutor extends AgentExecutor {
    // Build an agent executor
    static A2AExecutorBuilder builder() { ... }

    // Get the ChatClient for LLM interactions
    ChatClient getChatClient();

    // Get the system prompt for the agent
    String getSystemPrompt();

    // Generate response from user input
    default List<Part<?>> generateResponse(String userInput) { ... }

    // Execute synchronously
    Message executeSynchronous(Message request);

    // Execute with streaming
    default Flux<Message> executeStreaming(Message request) { ... }
}
```

### A2AClient

Client for calling remote A2A agents:

```java
public interface A2AClient {
    // Get agent metadata
    AgentCard getAgentCard();

    // Send a message to the agent
    Message sendMessage(Message request);

    // Send a message with streaming response
    Flux<Message> streamMessage(Message request);
}
```

### A2AServer

Server that exposes agents via HTTP/JSON-RPC:

```java
public interface A2AServer {
    // Get agent metadata
    AgentCard getAgentCard();

    // Process an A2A request
    Object handleRequest(A2ARequest request);
}
```

## JSON-RPC Protocol

Agents are exposed via JSON-RPC 2.0 over HTTP.

### Send Message

```json
{
  "jsonrpc": "2.0",
  "method": "sendMessage",
  "params": {
    "message": {
      "role": "user",
      "parts": [{"text": "Hello, agent!"}]
    }
  },
  "id": 1
}
```

### Get Agent Card

```json
{
  "jsonrpc": "2.0",
  "method": "getAgentCard",
  "params": {},
  "id": 1
}
```

## Testing

Run the integration tests:

```bash
# Run all tests
mvn test

# Run integration tests only
cd spring-ai-a2a-integration-tests
mvn test
```

## Project Status

**Version**: 0.1.0-SNAPSHOT
**Status**: Active Development

### Recent Changes (2026-01-13)

- Removed spring-ai-agents dependency
- Migrated to use A2A Java SDK directly
- Renamed `A2AAgentModel` to `A2AExecutor` for better clarity
- Simplified architecture with direct SDK usage

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting pull requests.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Resources

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## Support

For questions and support:
- GitHub Issues: [Report issues](https://github.com/your-org/spring-ai-a2a/issues)
- Discussions: [Join discussions](https://github.com/your-org/spring-ai-a2a/discussions)
