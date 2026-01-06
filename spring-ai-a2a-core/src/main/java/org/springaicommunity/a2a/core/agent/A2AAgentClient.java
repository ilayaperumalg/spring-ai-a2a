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

package org.springaicommunity.a2a.core.agent;

import reactor.core.publisher.Flux;

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;

/**
 * Interface for A2A (Agent-to-Agent) client implementations.
 *
 * <p>
 * This interface defines the contract for communicating with remote A2A agents. It
 * extends {@link A2AAgent} to provide agent metadata access and adds client-specific
 * operations for message exchange.
 *
 * <p>
 * Implementations of this interface handle:
 * <ul>
 * <li>Agent discovery and card retrieval</li>
 * <li>Synchronous message exchange via {@link #sendMessage(A2ARequest)}</li>
 * <li>Streaming message exchange via {@link #sendMessageStreaming(A2ARequest)}</li>
 * <li>Protocol-level communication (JSON-RPC, HTTP, etc.)</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre class="code">
 * // Create a client for a remote agent
 * A2AAgentClient weatherAgent = DefaultA2AAgentClient.builder()
 *     .agentUrl("http://localhost:10001/a2a")
 *     .build();
 *
 * // Send a message
 * A2AResponse response = weatherAgent.sendMessage(
 *     A2ARequest.of("What's the weather in San Francisco?")
 * );
 *
 * // Access agent metadata
 * AgentCard card = weatherAgent.getAgentCard();
 * System.out.println("Connected to: " + card.name());
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AAgent
 * @see A2ARequest
 * @see A2AResponse
 */
public interface A2AAgentClient extends A2AAgent {

	/**
	 * Send a message to this agent and receive a response.
	 * <p>
	 * This method provides synchronous communication with the remote agent. The
	 * implementation will block until a response is received or an error occurs.
	 * @param request the request message to send
	 * @return the response from the agent
	 * @throws RuntimeException if communication fails
	 */
	A2AResponse sendMessage(A2ARequest request);

	/**
	 * Send a message to this agent and receive a streaming response.
	 * <p>
	 * This method provides asynchronous streaming communication with the remote agent.
	 * The response is returned as a reactive stream that emits response chunks as they
	 * become available.
	 * <p>
	 * Default implementation converts the synchronous response to a single-element flux.
	 * Implementations that support true streaming should override this method.
	 * @param request the request message to send
	 * @return a flux of response chunks
	 */
	default Flux<A2AResponse> sendMessageStreaming(A2ARequest request) {
		return Flux.just(sendMessage(request));
	}

}
