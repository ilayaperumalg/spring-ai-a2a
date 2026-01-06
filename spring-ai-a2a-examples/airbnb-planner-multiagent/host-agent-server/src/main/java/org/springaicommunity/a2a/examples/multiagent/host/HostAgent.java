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

package org.springaicommunity.a2a.examples.multiagent.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Host Agent Application.
 *
 * <p>
 * This application serves as the orchestration layer for the multi-agent Airbnb travel
 * planner. It:
 * <ul>
 * <li>Discovers and registers remote specialized agents (weather and accommodation)</li>
 * <li>Routes user queries to appropriate agents using A2A protocol</li>
 * <li>Combines responses from multiple agents when needed</li>
 * <li>Provides a REST API for user interaction</li>
 * </ul>
 *
 * <p>
 * Configuration via environment variables:
 * <ul>
 * <li>OPENAI_API_KEY - Your OpenAI API key (optional, for orchestration logic)</li>
 * <li>WEATHER_AGENT_URL - URL of the weather agent (default: http://localhost:10001/a2a)
 * </li>
 * <li>AIRBNB_AGENT_URL - URL of the Airbnb agent (default: http://localhost:10002/a2a)
 * </li>
 * <li>SERVER_PORT - Server port (default: 8080)</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class HostAgent {

	private static final Logger logger = LoggerFactory.getLogger(HostAgent.class);

	public static void main(String[] args) {
		SpringApplication.run(HostAgent.class, args);
	}

	/**
	 * OpenAI API bean (optional).
	 * @return OpenAiApi instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public OpenAiApi openAiApi() {
		return OpenAiApi.builder().apiKey(getApiKey()).build();
	}

	/**
	 * OpenAI ChatModel bean (optional).
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
	 * ChatClient bean (optional, for orchestration logic).
	 * @param chatModel the OpenAiChatModel instance
	 * @return ChatClient instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public ChatClient chatClient(OpenAiChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}

	/**
	 * CommandLineRunner to register remote agents on startup.
	 * @param agentRegistry the remote agent registry
	 * @param weatherAgentUrl the weather agent URL from configuration
	 * @param airbnbAgentUrl the Airbnb agent URL from configuration
	 * @return the command line runner
	 */
	@Bean
	public CommandLineRunner registerRemoteAgents(RemoteAgentRegistry agentRegistry,
			@Value("${weather.agent.url:http://localhost:10001/a2a}") String weatherAgentUrl,
			@Value("${airbnb.agent.url:http://localhost:10002/a2a}") String airbnbAgentUrl) {
		return args -> {
			logger.info("Registering remote agents...");

			try {
				logger.info("Connecting to Weather Agent at: {}", weatherAgentUrl);
				agentRegistry.registerAgent("weather", weatherAgentUrl);
				logger.info("✓ Weather Agent registered successfully");
			}
			catch (Exception e) {
				logger.error("✗ Failed to register Weather Agent: {}", e.getMessage());
				logger.warn("Weather functionality will be unavailable");
			}

			try {
				logger.info("Connecting to Airbnb Agent at: {}", airbnbAgentUrl);
				agentRegistry.registerAgent("airbnb", airbnbAgentUrl);
				logger.info("✓ Airbnb Agent registered successfully");
			}
			catch (Exception e) {
				logger.error("✗ Failed to register Airbnb Agent: {}", e.getMessage());
				logger.warn("Accommodation search functionality will be unavailable");
			}

			logger.info("Host Agent initialization complete!");
			logger.info("API available at: http://localhost:${server.port:8080}/api/query");
		};
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
