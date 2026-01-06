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
import io.a2a.spec.Part;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springframework.util.Assert;

/**
 * Client implementation for communicating with remote A2A agents.
 *
 * <p>
 * This class provides a Spring AI-friendly wrapper around the A2A Java SDK Client,
 * enabling communication with remote agents via the A2A protocol. It supports both
 * synchronous and streaming message exchange patterns.
 *
 * <p>
 * The client automatically discovers agent capabilities by fetching the agent card from
 * the remote agent's endpoint. This information is used to configure the appropriate
 * communication patterns and validate requests.
 *
 * <p>
 * Example usage:
 *
 * <pre class="code">
 * // Create a client for a remote agent
 * A2AAgentClient weatherAgent = DefaultA2AAgentClient.builder()
 *     .agentUrl("http://localhost:10001/a2a")
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // Send a message
 * A2AResponse response = weatherAgent.sendMessage(
 *     A2ARequest.of("What's the weather in San Francisco?")
 * );
 *
 * // Get agent information
 * AgentCard card = weatherAgent.getAgentCard();
 * System.out.println("Connected to: " + card.name());
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AAgentClient
 * @see A2ARequest
 * @see A2AResponse
 */
public final class DefaultA2AAgentClient implements A2AAgentClient {

	private final String agentUrl;

	private final AgentCard agentCard;

	private final Duration timeout;

	private final boolean streaming;

	private DefaultA2AAgentClient(Builder builder) {
		Assert.hasText(builder.agentUrl, "agentUrl cannot be null or empty");
		this.agentUrl = builder.agentUrl;
		this.timeout = builder.timeout;
		this.streaming = builder.streaming;

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
	public A2AResponse sendMessage(A2ARequest request) {
		Assert.notNull(request, "request cannot be null");

		// Convert Spring AI request to A2A message
		Message a2aMessage = convertToA2AMessage(request);

		// Use CountDownLatch to block until response is received
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		// Create event consumers
		List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, card) -> {
			if (event instanceof MessageEvent messageEvent) {
				responseRef.set(messageEvent.getMessage());
				latch.countDown();
			}
			else if (event instanceof TaskEvent taskEvent) {
				// Extract response from task artifacts
				if (taskEvent.getTask().artifacts() != null && !taskEvent.getTask().artifacts().isEmpty()) {
					List<Part<?>> parts = taskEvent.getTask().artifacts().get(0).parts();
					Message artifactMessage = Message.builder().role(Message.Role.AGENT).parts(parts).build();
					responseRef.set(artifactMessage);
					latch.countDown();
				}
			}
		});

		Consumer<Throwable> errorHandler = error -> {
			errorRef.set(error);
			latch.countDown();
		};

		// Create and use A2A client
		ClientConfig clientConfig = new ClientConfig.Builder().setStreaming(false)
			.setAcceptedOutputModes(List.of("text"))
			.build();

		Client client = Client.builder(this.agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(consumers)
			.streamingErrorHandler(errorHandler)
			.build();

		client.sendMessage(a2aMessage);

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

		// Convert A2A Message to Spring AI response
		Message a2aResponse = responseRef.get();
		if (a2aResponse == null) {
			throw new RuntimeException("No response received from A2A agent at: " + this.agentUrl);
		}

		return convertToA2AResponse(a2aResponse);
	}

	@Override
	public Flux<A2AResponse> sendMessageStreaming(A2ARequest request) {
		Assert.notNull(request, "request cannot be null");

		if (!this.streaming) {
			// Fall back to non-streaming
			return Flux.just(sendMessage(request));
		}

		// Convert Spring AI request to A2A message
		Message a2aMessage = convertToA2AMessage(request);

		// Create a sink for streaming responses
		Sinks.Many<A2AResponse> sink = Sinks.many().unicast().onBackpressureBuffer();

		// Create event consumers for streaming
		List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, card) -> {
			if (event instanceof MessageEvent messageEvent) {
				A2AResponse response = convertToA2AResponse(messageEvent.getMessage());
				sink.tryEmitNext(response);
			}
			else if (event instanceof TaskEvent taskEvent) {
				if (taskEvent.getTask().artifacts() != null && !taskEvent.getTask().artifacts().isEmpty()) {
					List<Part<?>> parts = taskEvent.getTask().artifacts().get(0).parts();
					Message artifactMessage = Message.builder().role(Message.Role.AGENT).parts(parts).build();
					A2AResponse response = convertToA2AResponse(artifactMessage);
					sink.tryEmitNext(response);
				}
				// Complete on task completion
				if (taskEvent.getTask().status() != null) {
					switch (taskEvent.getTask().status().state()) {
						case COMPLETED, FAILED, CANCELED -> sink.tryEmitComplete();
					}
				}
			}
		});

		Consumer<Throwable> errorHandler = error -> sink.tryEmitError(error);

		// Create and use A2A client for streaming
		ClientConfig clientConfig = new ClientConfig.Builder().setStreaming(true)
			.setAcceptedOutputModes(List.of("text"))
			.build();

		Client client = Client.builder(this.agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(consumers)
			.streamingErrorHandler(errorHandler)
			.build();

		client.sendMessage(a2aMessage);

		return sink.asFlux();
	}

	/**
	 * Convert Spring AI A2ARequest to A2A SDK Message.
	 */
	private Message convertToA2AMessage(A2ARequest request) {
		return Message.builder()
			.role(Message.Role.USER)
			.parts(request.getParts())
			.contextId(request.getContextId())
			.taskId(request.getTaskId())
			.build();
	}

	/**
	 * Convert A2A SDK Message to Spring AI A2AResponse.
	 */
	private A2AResponse convertToA2AResponse(Message message) {
		return A2AResponse.of(message.parts());
	}

	/**
	 * Create a new builder for DefaultA2AAgentClient.
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating DefaultA2AAgentClient instances.
	 */
	public static final class Builder {

		private String agentUrl;

		private Duration timeout = Duration.ofSeconds(30);

		private boolean streaming = false;

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
		 * Enable or disable streaming mode.
		 * @param streaming true to enable streaming
		 * @return this builder
		 */
		public Builder streaming(boolean streaming) {
			this.streaming = streaming;
			return this;
		}

		/**
		 * Build the A2AAgentClient.
		 * @return a new A2AAgentClient instance
		 */
		public A2AAgentClient build() {
			return new DefaultA2AAgentClient(this);
		}

	}

}
