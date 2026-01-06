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

package org.springaicommunity.a2a.server.agentexecution;

import io.a2a.server.agentexecution.AgentExecutor;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Adapter interface that bridges the A2A Java SDK's {@link AgentExecutor} with Spring
 * AI's {@link AgentExecutorLifecycle} and ChatClient integration.
 *
 * <p>
 * This interface serves as the <strong>Adapter</strong> between two different APIs:
 * <ul>
 * <li><strong>A2A SDK</strong>: Low-level {@link AgentExecutor} with
 * {@code execute(RequestContext, EventQueue)}</li>
 * <li><strong>Spring AI</strong>: High-level {@link AgentExecutorLifecycle} with
 * simplified hooks and {@link ChatClient} integration</li>
 * </ul>
 *
 * <p>
 * <strong>Design Pattern:</strong> This interface implements the <strong>Adapter
 * Pattern</strong> by combining both interfaces, allowing Spring AI agents to work
 * seamlessly with the A2A protocol without requiring separate adapter classes.
 *
 * <p>
 * <strong>Benefits:</strong>
 * <ul>
 * <li><strong>Simplified Usage</strong>: No need for separate adapter classes</li>
 * <li><strong>Clear Intent</strong>: The interface clearly states "I'm a Spring AI agent
 * that works with A2A"</li>
 * <li><strong>Direct Integration</strong>: Can be used directly with
 * {@link org.springframework.ai.a2a.server.DefaultA2AAgentServer}</li>
 * <li><strong>Type Safety</strong>: Single interface for all Spring AI A2A agents</li>
 * </ul>
 *
 * <p>
 * Implementations provide:
 * <ul>
 * <li>A {@link ChatClient} instance for LLM interactions</li>
 * <li>A system prompt that defines the agent's behavior</li>
 * <li>Lifecycle hooks ({@link AgentExecutorLifecycle}) for simplified execution</li>
 * <li>A2A protocol methods ({@link AgentExecutor}) for protocol compatibility</li>
 * </ul>
 *
 * <p>
 * Example implementation:
 *
 * <pre>
 * public class WeatherAgent extends DefaultSpringAIAgentExecutor {
 *   public WeatherAgent(ChatClient chatClient) {
 *     super(chatClient);
 *   }
 *
 *   public String getSystemPrompt() {
 *     return "You are a weather assistant...";
 *   }
 * }
 * </pre>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * // Create your Spring AI agent
 * SpringAIAgentExecutor weatherAgent = new WeatherAgent(chatClient);
 *
 * // Use directly with A2A server (no adapter needed!)
 * DefaultA2AAgentServer server = new DefaultA2AAgentServer(agentCard, weatherAgent);
 * </pre>
 *
 * <p>
 * <strong>Adapter Responsibilities (handled by implementations):</strong>
 * <ol>
 * <li>Translate A2A's {@code execute(RequestContext, EventQueue)} to lifecycle hooks</li>
 * <li>Manage task lifecycle (submit, start, complete)</li>
 * <li>Convert lifecycle results to A2A artifacts</li>
 * <li>Handle errors and cancellation</li>
 * </ol>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see DefaultSpringAIAgentExecutor
 * @see AgentExecutorLifecycle
 * @see AgentExecutor
 * @see ChatClient
 */
public interface SpringAIAgentExecutor extends AgentExecutor, AgentExecutorLifecycle {

	/**
	 * Get the ChatClient instance used by this agent.
	 * <p>
	 * The ChatClient provides access to the underlying LLM and can be used for advanced
	 * interactions beyond the default execution logic.
	 * @return the ChatClient instance
	 */
	ChatClient getChatClient();

	/**
	 * Get the system prompt for this agent.
	 * <p>
	 * The system prompt defines the agent's behavior, capabilities, and response format.
	 * It is used to prime the LLM before processing user requests.
	 * @return the system prompt
	 */
	String getSystemPrompt();

}
