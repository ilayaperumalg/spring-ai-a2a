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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TaskState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springaicommunity.a2a.core.MessageUtils;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link A2AClient} for communicating with remote A2A agents.
 *
 * <p>This client uses A2A Java SDK types directly for message-based communication.
 * It wraps the A2A Java SDK Client to enable both blocking and streaming execution patterns.
 *
 * <p>The client automatically discovers agent capabilities by fetching the agent card from
 * the remote agent's endpoint during initialization. This information is used to configure
 * the appropriate communication patterns.
 *
 * <p><strong>Example - Blocking Execution:</strong>
 * <pre class="code">
 * // Create a client for a remote agent
 * A2AClient weatherAgent = DefaultA2AClient.builder()
 *     .agentUrl("http://localhost:10001/a2a")
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // Create a message
 * Message request = Message.builder()
 *     .role(Message.Role.USER)
 *     .parts(List.of(new TextPart("What's the weather in San Francisco?")))
 *     .build();
 *
 * // Execute task (blocking)
 * Message response = weatherAgent.sendMessage(request);
 * System.out.println(MessageUtils.extractText(response.parts()));
 * </pre>
 *
 * <p><strong>Example - Streaming Execution:</strong>
 * <pre class="code">
 * // Create a message
 * Message request = Message.builder()
 *     .role(Message.Role.USER)
 *     .parts(List.of(new TextPart("Analyze weather patterns for the past year")))
 *     .build();
 *
 * // Stream task with progress updates
 * Flux&lt;Message&gt; stream = weatherAgent.streamMessage(request);
 *
 * stream.subscribe(message -> {
 *     System.out.println("Response: " + MessageUtils.extractText(message.parts()));
 * });
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AClient
 * @see Message
 */
public final class DefaultA2AClient implements A2AClient {

	private final String agentUrl;

	private final AgentCard agentCard;

	private final Duration timeout;

	private DefaultA2AClient(Builder builder) {
		Assert.hasText(builder.agentUrl, "agentUrl cannot be null or empty");
		this.agentUrl = builder.agentUrl;
		this.timeout = builder.timeout;

		// Discover agent card
		this.agentCard = discoverAgentCard(this.agentUrl);
	}

	/**
	 * Discover the agent card from the remote agent.
	 * @param agentUrl the agent URL
	 * @return the agent card
	 */
	private AgentCard discoverAgentCard(String agentUrl) {
		try {
			A2ACardResolver cardResolver = new A2ACardResolver(agentUrl);
			return cardResolver.getAgentCard();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to discover agent card from: " + agentUrl, e);
		}
	}

	@Override
	public AgentCard getAgentCard() {
		return this.agentCard;
	}

	/**
	 * Get the agent URL.
	 * @return the agent URL
	 */
	public String getAgentUrl() {
		return this.agentUrl;
	}

	@Override
	public Message sendMessage(Message message) {
		Assert.notNull(message, "message cannot be null");

		// Use CountDownLatch to block until response is received
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		// Create event consumers
		List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, card) -> {
			if (event instanceof MessageEvent messageEvent) {
				// Store the message directly
				responseRef.set(messageEvent.getMessage());
				latch.countDown();
			}
			else if (event instanceof TaskEvent taskEvent) {
				// For task completion, extract message from artifacts
				if (isTaskComplete(taskEvent)) {
					Message response = extractMessageFromTaskEvent(taskEvent);
					responseRef.set(response);
					latch.countDown();
				}
			}
		});

		Consumer<Throwable> errorHandler = error -> {
			errorRef.set(error);
			latch.countDown();
		};

		// Create and use A2A client
		ClientConfig clientConfig = new ClientConfig.Builder()
			.setStreaming(false)
			.setAcceptedOutputModes(List.of("text"))
			.build();

		Client client = Client.builder(this.agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(consumers)
			.streamingErrorHandler(errorHandler)
			.build();

		client.sendMessage(message);

		try {
			// Wait for response with timeout
			if (!latch.await(this.timeout.toSeconds(), TimeUnit.SECONDS)) {
				throw new RuntimeException("Timeout waiting for A2A agent response after " + this.timeout.toSeconds()
						+ " seconds from: " + this.agentUrl);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for A2A agent response from: " + this.agentUrl, e);
		}

		// Check for errors
		if (errorRef.get() != null) {
			throw new RuntimeException("Error from A2A agent at: " + this.agentUrl, errorRef.get());
		}

		// Return the response
		Message response = responseRef.get();
		if (response == null) {
			throw new RuntimeException("No response received from A2A agent at: " + this.agentUrl);
		}

		return response;
	}

	@Override
	public Flux<Message> streamMessage(Message message) {
		Assert.notNull(message, "message cannot be null");

		// Create a sink for streaming responses
		Sinks.Many<Message> sink = Sinks.many().unicast().onBackpressureBuffer();

		// Create event consumers for streaming
		List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, card) -> {
			if (event instanceof TaskEvent taskEvent) {
				// Extract message from task event
				Message response = extractMessageFromTaskEvent(taskEvent);
				if (response != null) {
					sink.tryEmitNext(response);
				}

				// Complete stream on task completion
				if (isTaskComplete(taskEvent)) {
					sink.tryEmitComplete();
				}
			}
			else if (event instanceof MessageEvent messageEvent) {
				// Handle message event
				sink.tryEmitNext(messageEvent.getMessage());
				sink.tryEmitComplete();
			}
		});

		Consumer<Throwable> errorHandler = error -> sink.tryEmitError(error);

		// Create and use A2A client for streaming
		ClientConfig clientConfig = new ClientConfig.Builder()
			.setStreaming(true)
			.setAcceptedOutputModes(List.of("text"))
			.build();

		Client client = Client.builder(this.agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(consumers)
			.streamingErrorHandler(errorHandler)
			.build();

		client.sendMessage(message);

		return sink.asFlux();
	}

	/**
	 * Extract Message from TaskEvent artifacts.
	 */
	private Message extractMessageFromTaskEvent(TaskEvent taskEvent) {
		// Extract message from task artifacts if available
		if (taskEvent.getTask().artifacts() != null && !taskEvent.getTask().artifacts().isEmpty()) {
			// Use the first artifact as the response message
			// Note: A2A spec uses AGENT role for agent responses
			return Message.builder()
				.role(Message.Role.AGENT)
				.parts(taskEvent.getTask().artifacts().get(0).parts())
				.build();
		}
		return null;
	}

	/**
	 * Check if task is in a terminal state.
	 */
	private boolean isTaskComplete(TaskEvent taskEvent) {
		if (taskEvent.getTask().status() == null || taskEvent.getTask().status().state() == null) {
			return false;
		}
		TaskState state = taskEvent.getTask().status().state();
		return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELED;
	}

	/**
	 * Create a new builder for DefaultA2AClient.
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating DefaultA2AClient instances.
	 */
	public static final class Builder {

		private String agentUrl;

		private Duration timeout = Duration.ofSeconds(30);

		private Builder() {
		}

		/**
		 * Set the agent URL.
		 * @param agentUrl the A2A endpoint URL (e.g., "http://localhost:10001/a2a")
		 * @return this builder
		 */
		public Builder agentUrl(String agentUrl) {
			this.agentUrl = agentUrl;
			return this;
		}

		/**
		 * Set the request timeout.
		 * @param timeout the timeout duration
		 * @return this builder
		 */
		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Build the A2AClient.
		 * @return a new A2AClient instance
		 */
		public A2AClient build() {
			return new DefaultA2AClient(this);
		}

	}

}
