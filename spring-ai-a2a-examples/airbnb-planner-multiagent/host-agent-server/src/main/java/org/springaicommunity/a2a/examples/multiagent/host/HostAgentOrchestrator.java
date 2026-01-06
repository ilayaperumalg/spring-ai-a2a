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

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springaicommunity.a2a.core.agent.A2AAgentClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Host Agent Orchestrator that routes user requests to specialized remote agents.
 *
 * <p>
 * This orchestrator uses the A2A Agent architecture to intelligently route user queries
 * to the appropriate specialized agent (weather or accommodation) and combines their
 * responses.
 *
 * <p>
 * The orchestrator demonstrates:
 * <ul>
 * <li>Multi-agent coordination using A2A protocol</li>
 * <li>Direct agent communication via {@link A2AAgentClient}</li>
 * <li>Intelligent request routing based on query content</li>
 * <li>Combining responses from multiple agents</li>
 * <li>Optional LLM-based routing for ambiguous queries</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Service
public class HostAgentOrchestrator {

	private final RemoteAgentRegistry agentRegistry;

	private final ChatClient orchestratorClient;

	/**
	 * Create a new HostAgentOrchestrator.
	 * @param agentRegistry the registry of remote agents
	 * @param chatModel the chat model for orchestration logic (optional)
	 */
	public HostAgentOrchestrator(RemoteAgentRegistry agentRegistry, @Autowired(required = false) ChatModel chatModel) {
		this.agentRegistry = agentRegistry;
		// Build orchestrator client if ChatModel is available
		// This client is used for routing decisions
		this.orchestratorClient = (chatModel != null) ? ChatClient.builder(chatModel).build() : null;
	}

	/**
	 * Process a user request by routing it to the appropriate agent(s).
	 * @param userMessage the user's query
	 * @return the response from the agent(s)
	 */
	public String processRequest(String userMessage) {
		// Determine which agent(s) to invoke based on the query
		String agentType = determineAgentType(userMessage);

		switch (agentType) {
			case "weather":
				return queryWeatherAgent(userMessage);
			case "airbnb":
				return queryAirbnbAgent(userMessage);
			case "both":
				// User query requires both agents
				return queryCombinedAgents(userMessage);
			default:
				return "I'm not sure how to help with that. I can assist with weather information or accommodation searches.";
		}
	}

	/**
	 * Determine which agent(s) should handle the user's request.
	 */
	private String determineAgentType(String userMessage) {
		String lowerMessage = userMessage.toLowerCase();

		boolean needsWeather = lowerMessage.contains("weather") || lowerMessage.contains("temperature")
				|| lowerMessage.contains("forecast") || lowerMessage.contains("rain") || lowerMessage.contains("sunny")
				|| lowerMessage.contains("climate");

		boolean needsAirbnb = lowerMessage.contains("room") || lowerMessage.contains("accommodation")
				|| lowerMessage.contains("stay") || lowerMessage.contains("hotel") || lowerMessage.contains("airbnb")
				|| lowerMessage.contains("lodging") || lowerMessage.contains("place to stay");

		if (needsWeather && needsAirbnb) {
			return "both";
		}
		else if (needsWeather) {
			return "weather";
		}
		else if (needsAirbnb) {
			return "airbnb";
		}
		else {
			// Use orchestrator LLM to determine intent if available
			if (this.orchestratorClient != null) {
				String intent = this.orchestratorClient.prompt().system("""
						Analyze the user's query and determine if it's about:
						- "weather": weather, temperature, forecast, climate
						- "airbnb": accommodations, lodging, rooms, places to stay
						- "both": requires both weather AND accommodation info
						- "unknown": doesn't match either category

						Respond with ONLY ONE WORD: weather, airbnb, both, or unknown
						""").user(userMessage).call().content().toLowerCase().trim();

				return intent.contains("weather") ? "weather"
						: intent.contains("airbnb") ? "airbnb" : intent.contains("both") ? "both" : "unknown";
			}
			return "unknown";
		}
	}

	/**
	 * Query the weather agent.
	 */
	private String queryWeatherAgent(String userMessage) {
		A2AAgentClient weatherAgent = this.agentRegistry.getClient("weather");
		A2AResponse response = weatherAgent.sendMessage(A2ARequest.of(userMessage));
		return response.getTextContent();
	}

	/**
	 * Query the Airbnb agent.
	 */
	private String queryAirbnbAgent(String userMessage) {
		A2AAgentClient airbnbAgent = this.agentRegistry.getClient("airbnb");
		A2AResponse response = airbnbAgent.sendMessage(A2ARequest.of(userMessage));
		return response.getTextContent();
	}

	/**
	 * Query both agents and combine their responses.
	 */
	private String queryCombinedAgents(String userMessage) {
		// Query both agents in parallel (or sequentially)
		String weatherResponse = queryWeatherAgent(userMessage);
		String airbnbResponse = queryAirbnbAgent(userMessage);

		// Combine the responses
		StringBuilder combined = new StringBuilder();
		combined.append("# Travel Planning Results\n\n");

		if (weatherResponse != null && !weatherResponse.isEmpty()) {
			combined.append(weatherResponse).append("\n\n---\n\n");
		}

		if (airbnbResponse != null && !airbnbResponse.isEmpty()) {
			combined.append(airbnbResponse);
		}

		return combined.toString();
	}

}
