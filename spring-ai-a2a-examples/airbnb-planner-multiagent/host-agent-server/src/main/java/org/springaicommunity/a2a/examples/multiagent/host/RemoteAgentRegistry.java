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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.a2a.spec.AgentCard;

import org.springaicommunity.a2a.core.agent.A2AAgent;
import org.springaicommunity.a2a.core.agent.A2AAgentClient;
import org.springaicommunity.a2a.core.agent.DefaultA2AAgentClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Registry for remote A2A agents.
 *
 * <p>
 * This component provides a simplified API for registering and accessing remote A2A
 * agents. It manages:
 * <ul>
 * <li>A2A agent registration and discovery</li>
 * <li>Direct access to A2AAgent instances</li>
 * <li>Agent card retrieval</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Component
public class RemoteAgentRegistry {

	private final Map<String, A2AAgent> agents = new ConcurrentHashMap<>();

	/**
	 * Register a remote agent with the registry.
	 * @param agentName a logical name for the agent (e.g., "weather", "airbnb")
	 * @param agentUrl the A2A endpoint URL for the agent
	 * @return the AgentCard for the registered agent
	 * @throws RuntimeException if agent card cannot be fetched
	 */
	public AgentCard registerAgent(String agentName, String agentUrl) {
		Assert.hasText(agentName, "agentName cannot be null or empty");
		Assert.hasText(agentUrl, "agentUrl cannot be null or empty");

		A2AAgentClient client = DefaultA2AAgentClient.builder().agentUrl(agentUrl).build();
		this.agents.put(agentName, client);

		return client.getAgentCard();
	}

	/**
	 * Get the A2AAgent for a registered agent.
	 * <p>
	 * This provides direct access to the A2A agent for advanced usage.
	 * @param agentName the logical name of the agent
	 * @return the A2AAgent instance
	 * @throws IllegalArgumentException if agent is not registered
	 */
	public A2AAgent getAgent(String agentName) {
		A2AAgent agent = this.agents.get(agentName);
		if (agent == null) {
			throw new IllegalArgumentException("Agent not registered: " + agentName);
		}
		return agent;
	}

	/**
	 * Get the A2AAgentClient for a registered agent.
	 * @param agentName the logical name of the agent
	 * @return the A2AAgentClient for the agent
	 * @throws IllegalArgumentException if agent is not registered
	 */
	public A2AAgentClient getClient(String agentName) {
		A2AAgent agent = getAgent(agentName);
		// The agent from registry is already an A2AAgentClient
		if (agent instanceof A2AAgentClient agentClient) {
			return agentClient;
		}
		throw new IllegalStateException("Agent is not an A2AAgentClient: " + agentName);
	}

	/**
	 * Get the AgentCard for a registered agent.
	 * @param agentName the logical name of the agent
	 * @return the AgentCard for the agent
	 * @throws IllegalArgumentException if agent is not registered
	 */
	public AgentCard getAgentCard(String agentName) {
		A2AAgent agent = getAgent(agentName);
		return agent.getAgentCard();
	}

	/**
	 * Check if an agent is registered.
	 * @param agentName the logical name of the agent
	 * @return true if the agent is registered
	 */
	public boolean isRegistered(String agentName) {
		return this.agents.containsKey(agentName);
	}

}
