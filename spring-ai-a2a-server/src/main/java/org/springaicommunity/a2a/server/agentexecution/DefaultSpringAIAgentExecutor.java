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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Default implementation of {@link SpringAIAgentExecutor} that provides Spring AI
 * ChatClient integration and adapter logic.
 *
 * <p>
 * This class implements {@link SpringAIAgentExecutor}, making it easy to create Spring
 * AI-based agents with minimal code. It serves as the adapter between the A2A SDK's
 * low-level {@code AgentExecutor} interface and Spring AI's high-level
 * {@link AgentExecutorLifecycle} interface, while also providing ChatClient integration.
 *
 * <p>
 * <strong>Adapter Responsibilities:</strong>
 * <ul>
 * <li>Translates A2A SDK's {@code execute(RequestContext, EventQueue)} to lifecycle
 * hooks</li>
 * <li>Manages task lifecycle (submit, start, complete)</li>
 * <li>Converts lifecycle results to A2A artifacts</li>
 * <li>Handles errors and cancellation</li>
 * </ul>
 *
 * <p>
 * <strong>Usage with A2A Protocol:</strong> This class can be used directly with
 * {@link org.springframework.ai.a2a.server.DefaultA2AAgentServer}:
 *
 * <pre>
 * // Create your Spring AI agent
 * SpringAIAgentExecutor weatherAgent = new WeatherAgentExecutor(chatClient);
 *
 * // Use directly with A2A server (no adapter needed!)
 * DefaultA2AAgentServer server = new DefaultA2AAgentServer(agentCard, weatherAgent);
 * </pre>
 *
 * <p>
 * Implementations only need to provide a system prompt via {@link #getSystemPrompt()},
 * and the ChatClient handles the rest.
 *
 * <p>
 * Example implementation:
 *
 * <pre>
 * public class MyAgent extends DefaultSpringAIAgentExecutor {
 *   public MyAgent(ChatClient chatClient) {
 *     super(chatClient);
 *   }
 *
 *   public String getSystemPrompt() {
 *     return "You are a helpful assistant that...";
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more complex scenarios where you need full control over the ChatClient interaction
 * or want to use additional Spring AI features, override
 * {@link AgentExecutorLifecycle#onExecute(String, RequestContext, TaskUpdater)}.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see SpringAIAgentExecutor
 * @see ChatClient
 */
public abstract class DefaultSpringAIAgentExecutor implements SpringAIAgentExecutor {

	private final ChatClient chatClient;

	/**
	 * Create a new DefaultSpringAIAgentExecutor with the given ChatClient.
	 * @param chatClient the ChatClient for LLM interactions
	 */
	protected DefaultSpringAIAgentExecutor(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Get the ChatClient instance.
	 * <p>
	 * Subclasses can access the ChatClient for advanced usage scenarios.
	 * @return the ChatClient
	 */
	@Override
	public ChatClient getChatClient() {
		return this.chatClient;
	}

	/**
	 * Get the system prompt for this agent.
	 * <p>
	 * Subclasses must override this method to provide their own system prompt that
	 * defines the agent's behavior and capabilities.
	 * @return the system prompt
	 */
	@Override
	public abstract String getSystemPrompt();

	/**
	 * Execute the agent logic using ChatClient.
	 * <p>
	 * Default implementation uses the system prompt from {@link #getSystemPrompt()} and
	 * the user input to call the ChatClient. Subclasses can override this method for more
	 * complex interactions.
	 * @param userInput the user's input extracted from the request
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 * @return a list of response parts (e.g., TextPart, ImagePart)
	 * @throws Exception if execution fails
	 */
	@Override
	public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) throws Exception {
		String systemPrompt = getSystemPrompt();
		String response = this.chatClient.prompt().system(systemPrompt).user(userInput).call().content();
		return List.of(new TextPart(response));
	}

	/**
	 * Execute the agent using the A2A SDK interface.
	 * <p>
	 * This method adapts the A2A SDK's execute method to Spring AI's
	 * {@link AgentExecutorLifecycle} by:
	 * <ol>
	 * <li>Creating a {@link TaskUpdater} from the context and event queue</li>
	 * <li>Managing task lifecycle (submit, start, complete)</li>
	 * <li>Delegating to {@link #onExecute(String, RequestContext, TaskUpdater)}</li>
	 * <li>Converting results to A2A artifacts</li>
	 * <li>Calling {@link #onComplete(RequestContext, TaskUpdater)} for
	 * post-processing</li>
	 * <li>Handling errors via
	 * {@link #onError(Exception, RequestContext, TaskUpdater)}</li>
	 * </ol>
	 * @param context the request context from A2A SDK
	 * @param eventQueue the event queue for publishing events
	 * @throws io.a2a.spec.JSONRPCError if execution fails
	 */
	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws io.a2a.spec.JSONRPCError {
		TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);

		try {
			// Submit and start task if it's a new task
			if (context.getTask() == null) {
				taskUpdater.submit();
			}
			taskUpdater.startWork();

			// Extract user input from the request
			String userInput = context.getUserInput(" ");

			// Delegate to lifecycle hook
			List<Part<?>> responseParts = this.onExecute(userInput, context, taskUpdater);

			// Add response as artifacts
			if (responseParts != null && !responseParts.isEmpty()) {
				taskUpdater.addArtifact(responseParts);
			}

			// Call completion hook before marking task as complete
			this.onComplete(context, taskUpdater);

			// Complete the task
			taskUpdater.complete();
		}
		catch (Exception e) {
			this.onError(e, context, taskUpdater);
		}
	}

	/**
	 * Cancel the agent execution using the A2A SDK interface.
	 * <p>
	 * This method adapts the A2A SDK's cancel method to Spring AI's
	 * {@link AgentExecutorLifecycle#onCancel(RequestContext, TaskUpdater)}.
	 * @param context the request context from A2A SDK
	 * @param eventQueue the event queue for publishing events
	 * @throws io.a2a.spec.JSONRPCError if cancellation fails
	 */
	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws io.a2a.spec.JSONRPCError {
		TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
		this.onCancel(context, taskUpdater);
	}

	/**
	 * Executes the agent synchronously and returns the response.
	 * <p>
	 * This method provides synchronous execution support for agents. It:
	 * <ul>
	 * <li>Creates an EventQueue to collect responses</li>
	 * <li>Invokes the execute method via A2A SDK interface</li>
	 * <li>Waits for task completion and collects all artifacts</li>
	 * <li>Returns the collected response synchronously</li>
	 * </ul>
	 * @param request the A2A request
	 * @return the A2A response
	 */
	@Override
	public A2AResponse executeSynchronous(A2ARequest request) {
		try {
			// Create a synchronous event queue to collect responses
			SynchronousEventQueue eventQueue = new SynchronousEventQueue();

			// Build request context from A2ARequest
			RequestContext.Builder contextBuilder = new RequestContext.Builder();

			// Build message with contextId and taskId from request
			io.a2a.spec.Message message = io.a2a.spec.Message.builder(request.getMessage())
				.contextId(request.getContextId())
				.taskId(request.getTaskId())
				.build();

			// Create MessageSendParams from the message with contextId/taskId
			contextBuilder.setParams(new io.a2a.spec.MessageSendParams(message, null, null));

			RequestContext context = contextBuilder.build();

			// Execute the agent
			this.execute(context, eventQueue);

			// Wait for completion and collect response
			A2AResponse response = eventQueue.awaitCompletion(30);

			return response;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to execute agent", e);
		}
	}

	/**
	 * Synchronous EventQueue implementation that collects events and waits for task
	 * completion.
	 */
	private static class SynchronousEventQueue extends EventQueue {

		private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

		private final List<Part<?>> responseParts = new ArrayList<>();

		private volatile boolean completed = false;

		private volatile boolean failed = false;

		private volatile Exception error;

		@Override
		public void enqueueEvent(Event event) {
			this.events.offer(event);
			processEvent(event);
		}

		private void processEvent(Event event) {
			if (event instanceof TaskArtifactUpdateEvent artifactEvent) {
				Artifact artifact = artifactEvent.artifact();
				if (artifact != null && artifact.parts() != null) {
					synchronized (this.responseParts) {
						this.responseParts.addAll(artifact.parts());
					}
				}
			}
			else if (event instanceof TaskStatusUpdateEvent statusEvent) {
				TaskState state = statusEvent.status().state();
				if (state == TaskState.COMPLETED) {
					synchronized (this) {
						this.completed = true;
						this.notifyAll();
					}
				}
				else if (state == TaskState.FAILED || state == TaskState.CANCELED) {
					synchronized (this) {
						this.failed = true;
						this.error = new RuntimeException("Task ended with state: " + state);
						this.notifyAll();
					}
				}
			}
		}

		public A2AResponse awaitCompletion(int timeoutSeconds) throws InterruptedException {
			synchronized (this) {
				long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

				while (!this.completed && !this.failed) {
					long remaining = deadline - System.currentTimeMillis();
					if (remaining <= 0) {
						throw new RuntimeException("Agent execution timed out after " + timeoutSeconds + " seconds");
					}
					this.wait(remaining);
				}

				if (this.failed) {
					throw new RuntimeException("Agent execution failed", this.error);
				}

				synchronized (this.responseParts) {
					return A2AResponse.of(new ArrayList<>(this.responseParts));
				}
			}
		}

		@Override
		public EventQueue tap() {
			throw new UnsupportedOperationException("Synchronous event queue does not support tapping");
		}

		@Override
		public void awaitQueuePollerStart() throws InterruptedException {
			// No-op for synchronous execution
		}

		@Override
		public void signalQueuePollerStarted() {
			// No-op for synchronous execution
		}

		@Override
		public void close() {
			// No-op for synchronous execution
		}

		@Override
		public void close(boolean immediate) {
			// No-op for synchronous execution
		}

		@Override
		public void close(boolean immediate, boolean notifyParent) {
			// No-op for synchronous execution
		}

	}

}
