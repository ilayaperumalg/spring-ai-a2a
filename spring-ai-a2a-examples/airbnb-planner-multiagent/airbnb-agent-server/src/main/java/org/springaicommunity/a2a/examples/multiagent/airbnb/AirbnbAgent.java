/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.a2a.examples.multiagent.airbnb;

import java.util.List;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springaicommunity.a2a.server.A2AAgentServer;
import org.springaicommunity.a2a.server.DefaultA2AAgentServer;
import org.springaicommunity.a2a.server.agentexecution.SpringAIAgentExecutor;

/**
 * Airbnb Agent Server Application.
 *
 * <p>
 * This application demonstrates how to create an A2A agent server using Spring AI for
 * accommodation search and recommendations:
 * <ul>
 * <li>Uses {@link org.springframework.ai.a2a.server.agentexecution.SpringAIAgentExecutor}
 * to wrap a custom {@link io.a2a.server.agentexecution.AgentExecutor}</li>
 * <li>Creates an {@link AgentCard} programmatically</li>
 * <li>Integrates with Spring AI {@link ChatClient} for LLM capabilities</li>
 * <li>Exposes A2A endpoints for agent-to-agent communication</li>
 * </ul>
 *
 * <p>
 * Environment variables required:
 * <ul>
 * <li>OPENAI_API_KEY - Your OpenAI API key</li>
 * <li>SERVER_PORT (optional) - Server port, defaults to 10002</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class AirbnbAgent {

	public static void main(String[] args) {
		SpringApplication.run(AirbnbAgent.class, args);
	}

	/**
	 * Create the AgentExecutor bean.
	 * <p>
	 * The AirbnbAgentExecutor extends DefaultSpringAIAgentExecutor which implements
	 * SpringAIAgentExecutor (combining AgentExecutor and AgentExecutorLifecycle),
	 * providing simplified lifecycle hooks for Spring AI agents.
	 * @param chatClient the ChatClient for LLM interactions
	 * @return the AgentExecutor
	 */
	@Bean
	public AgentExecutor agentExecutor(@Autowired(required = false) ChatClient chatClient) {
		return new AirbnbAgentExecutor(chatClient);
	}

	/**
	 * Create the AgentCard bean.
	 * <p>
	 * The AgentCard describes the agent's capabilities and supported interfaces.
	 * @param serverPort the server port
	 * @return the AgentCard
	 */
	@Bean
	public AgentCard agentCard(@Value("${server.port:10002}") int serverPort) {
		String agentUrl = "http://localhost:" + serverPort + "/a2a";
		return AgentCard.builder()
			.name("Airbnb Agent")
			.description("Searches and recommends accommodations based on location, dates, and guest requirements")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(AgentCapabilities.builder()
				.streaming(false)
				.pushNotifications(false)
				.stateTransitionHistory(false)
				.build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", agentUrl)))
			.skills(List.of(AgentSkill.builder()
				.id("accommodation-search")
				.name("Search Accommodations")
				.description(
						"Searches for available accommodations based on location, check-in/check-out dates, and number of guests")
				.tags(List.of("airbnb", "accommodation", "lodging", "booking"))
				.build()))
			.build();
	}

	/**
	 * Create the A2A Agent Server bean.
	 * <p>
	 * This exposes the A2A protocol endpoints for agent-to-agent communication.
	 * @param agentCard the agent card
	 * @param agentExecutor the agent executor
	 * @return the A2A agent server
	 */
	@Bean
	public A2AAgentServer a2aAgentServer(AgentCard agentCard, AgentExecutor agentExecutor) {
		// Cast AgentExecutor to SpringAIAgentExecutor
		if (agentExecutor instanceof SpringAIAgentExecutor springAIAgentExecutor) {
			return new DefaultA2AAgentServer(agentCard, springAIAgentExecutor);
		}
		throw new IllegalArgumentException("AgentExecutor must implement SpringAIAgentExecutor");
	}

	/**
	 * OpenAI API bean.
	 * @return OpenAiApi instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public OpenAiApi openAiApi() {
		return OpenAiApi.builder().apiKey(getApiKey()).build();
	}

	/**
	 * OpenAI ChatModel bean.
	 * @param api the OpenAiApi instance
	 * @return OpenAiChatModel instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public OpenAiChatModel openAiChatModel(OpenAiApi api) {
		return OpenAiChatModel.builder()
			.openAiApi(api)
			.defaultOptions(OpenAiChatOptions.builder().model(OpenAiApi.ChatModel.GPT_4_O_MINI).build())
			.build();
	}

	/**
	 * ChatClient bean.
	 * @param chatModel the OpenAiChatModel instance
	 * @return ChatClient instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public ChatClient chatClient(OpenAiChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}

	private ApiKey getApiKey() {
		String apiKey = System.getenv("OPENAI_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException(
					"You must provide an API key. Put it in an environment variable under the name OPENAI_API_KEY");
		}
		return new SimpleApiKey(apiKey);
	}

}
