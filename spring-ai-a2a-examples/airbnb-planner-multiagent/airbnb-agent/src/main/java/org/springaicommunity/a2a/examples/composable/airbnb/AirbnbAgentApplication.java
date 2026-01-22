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

package org.springaicommunity.a2a.examples.composable.airbnb;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springaicommunity.a2a.server.executor.AbstractA2AChatClientAgentExecutor;
import java.util.List;

/**
 * A2A agent for accommodation and lodging search.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
@SpringBootApplication
public class AirbnbAgentApplication {

	private static final String AIRBNB_SYSTEM_INSTRUCTION = """
		You are a specialized assistant for accommodation search and recommendations.
		Your primary function is to utilize the provided tools to search for accommodations and answer related questions.
		You must rely exclusively on these tools for information; do not invent listings or prices.
		Always use the available tools for accurate, up-to-date accommodation data.
		Ensure that your Markdown-formatted response includes all relevant tool output, with particular emphasis on providing direct links to listings.
		""";

	public static void main(String[] args) {
		SpringApplication.run(AirbnbAgentApplication.class, args);
	}

	/**
	 * Agent card with metadata and capabilities.
	 */
	@Bean
    public AgentCard agentCard() {
		return new AgentCard.Builder()
			.name("Airbnb Agent")
			.description("Helps with searching accommodations, hotels, airbnb, and lodging")
			.url("http://localhost:10002/a2a")
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder()
				.streaming(false)
				.build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(
				new AgentSkill.Builder()
					.id("accommodation_search")
					.name("Accommodation Search")
					.description("Provides hotel and accommodation recommendations for any location")
					.tags(List.of("accommodation", "hotel", "airbnb", "lodging"))
					.examples(List.of("Find hotels in Paris for 3 nights", "Recommend accommodations in Tokyo"))
					.build()
			))
			.protocolVersion("0.3.0")
			.build();
	}

	/**
	 * Chat client configured with accommodation tools.
	 */
	@Bean
    public ChatClient airbnbChatClient(ChatClient.Builder chatClientBuilder, AirbnbTools airbnbTools) {
		return chatClientBuilder.clone()
			.defaultSystem(AIRBNB_SYSTEM_INSTRUCTION)
			.defaultTools(airbnbTools)
			.build();
	}

	/**
	 * Agent executor for processing A2A requests.
	 */
	@Bean
    public AgentExecutor airbnbAgentExecutor(ChatClient airbnbChatClient) {
		return new AirbnbAgentExecutor(airbnbChatClient);
	}

	/**
	 * Agent executor extending AbstractA2AChatClientAgentExecutor.
	 */
	private static class AirbnbAgentExecutor extends AbstractA2AChatClientAgentExecutor {

		public AirbnbAgentExecutor(ChatClient chatClient) {
			super(chatClient);
		}

		@Override
		protected String processUserMessage(String userMessage) {
			return this.chatClient.prompt()
				.user(userMessage)
				.call()
				.content();
		}
	}

}
