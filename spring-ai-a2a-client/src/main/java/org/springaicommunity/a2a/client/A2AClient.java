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

package org.springaicommunity.a2a.client;

import io.a2a.spec.Message;
import reactor.core.publisher.Flux;

import org.springaicommunity.a2a.core.A2AEndpoint;

/**
 * Interface for A2A (Agent-to-Agent) client implementations using A2A SDK types directly.
 *
 * <p>
 * This interface defines the contract for communicating with remote A2A agents using
 * the A2A Java SDK Message types. It extends {@link A2AEndpoint} for agent metadata access
 * and provides methods for both blocking and streaming message execution.
 *
 * <p>
 * Implementations of this interface handle:
 * <ul>
 * <li>Agent discovery and card retrieval</li>
 * <li>Blocking message execution via {@link #sendMessage(Message)}</li>
 * <li>Streaming execution with progress tracking via {@link #streamMessage(Message)}</li>
 * <li>Protocol-level communication (JSON-RPC, HTTP, etc.)</li>
 * </ul>
 *
 * <p>
 * <strong>Execution Patterns:</strong>
 *
 * <p>
 * <strong>Blocking Execution</strong> ({@link #sendMessage(Message)}):
 * <ul>
 * <li>Synchronous, blocking API</li>
 * <li>Returns {@link Message} when complete</li>
 * <li>Best for simple queries that complete quickly</li>
 * <li>Uses A2A SDK types directly</li>
 * </ul>
 *
 * <p>
 * <strong>Streaming Execution</strong> ({@link #streamMessage(Message)}):
 * <ul>
 * <li>Asynchronous, reactive API using Flux</li>
 * <li>Emits {@link Message} chunks with progress updates</li>
 * <li>Best for long-running tasks requiring progress feedback</li>
 * <li>Supports task status tracking</li>
 * </ul>
 *
 * <p>
 * <strong>Example - Blocking Execution:</strong>
 *
 * <pre class="code">
 * // Create a client for a remote agent
 * A2AClient weatherAgent = DefaultA2AClient.builder()
 *     .agentUrl("http://localhost:10001/a2a")
 *     .build();
 *
 * // Create a message
 * Message request = Message.builder()
 *     .role(Message.Role.USER)
 *     .parts(List.of(new TextPart("What's the weather in San Francisco?")))
 *     .build();
 *
 * // Blocking execution
 * Message response = weatherAgent.sendMessage(request);
 * System.out.println(MessageUtils.extractText(response.parts()));
 *
 * // Access agent metadata
 * AgentCard card = weatherAgent.getAgentCard();
 * System.out.println("Connected to: " + card.name());
 * </pre>
 *
 * <p>
 * <strong>Example - Streaming with Progress:</strong>
 *
 * <pre class="code">
 * // Create a message
 * Message request = Message.builder()
 *     .role(Message.Role.USER)
 *     .parts(List.of(new TextPart("Analyze weather patterns for the past year")))
 *     .build();
 *
 * // Streaming execution for long-running tasks
 * Flux&lt;Message&gt; stream = weatherAgent.streamMessage(request);
 *
 * stream.subscribe(message -&gt; {
 *     System.out.println("Response: " + MessageUtils.extractText(message.parts()));
 * });
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AEndpoint
 * @see Message
 */
public interface A2AClient extends A2AEndpoint {

	/**
	 * Send a message to the remote A2A agent and wait for the complete response.
	 *
	 * <p>This is a synchronous, blocking operation that waits for the agent to
	 * complete processing and return the full response message.
	 *
	 * @param message the message to send (typically with USER role)
	 * @return the response message from the agent
	 * @throws RuntimeException if the request fails or times out
	 */
	Message sendMessage(Message message);

	/**
	 * Send a message to the remote A2A agent and stream responses.
	 *
	 * <p>This is an asynchronous, reactive operation that emits response messages
	 * as they become available. Useful for long-running tasks with progress updates.
	 *
	 * @param message the message to send (typically with USER role)
	 * @return a Flux of response messages from the agent
	 */
	Flux<Message> streamMessage(Message message);

	/**
	 * Check if this agent client is available and ready to process tasks.
	 *
	 * <p>Default implementation checks if the agent card was successfully retrieved.
	 * Implementations can override to provide more sophisticated availability checks
	 * (e.g., health checks, connectivity tests).
	 *
	 * @return true if the agent is available, false otherwise
	 */
	default boolean isAvailable() {
		return getAgentCard() != null;
	}

}
