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

package org.springaicommunity.a2a.server.executor;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for A2A AgentExecutors using Spring AI ChatClient.
 *
 * <p>This executor handles Task-based execution, managing the complete task lifecycle:
 * <ul>
 *   <li>Creates and submits task via {@link TaskUpdater}</li>
 *   <li>Extracts user message from A2A protocol {@link Message}</li>
 *   <li>Delegates to {@link #processUserMessage(String)} for agent-specific logic</li>
 *   <li>Wraps response as task artifact and completes the task</li>
 * </ul>
 *
 * <p>Implementations only need to provide the {@link #processUserMessage(String)} method
 * that takes a simple String and returns a String response. All A2A protocol complexity
 * and task management is handled by this base class.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public abstract class AbstractA2AChatClientAgentExecutor implements AgentExecutor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractA2AChatClientAgentExecutor.class);

	protected final ChatClient chatClient;

	protected AbstractA2AChatClientAgentExecutor(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Extracts text content from A2A message.
	 */
	private String extractTextFromMessage(Message message) {
		if (message == null || message.getParts() == null) {
			return "";
		}
		return message.getParts().stream()
			.filter(part -> part instanceof TextPart)
			.map(part -> ((TextPart) part).getText())
			.collect(Collectors.joining())
			.trim();
	}

	/**
	 * Process user message and return response text.
	 *
	 * <p>This is the only method implementations need to provide. The framework
	 * automatically handles:
	 * <ul>
	 *   <li>Task creation and lifecycle management (SUBMITTED → WORKING → COMPLETED)</li>
	 *   <li>Message extraction from A2A protocol</li>
	 *   <li>Wrapping response as task artifact</li>
	 *   <li>Error handling and JSON-RPC error responses</li>
	 * </ul>
	 *
	 * @param userMessage Extracted text from incoming A2A message
	 * @return Response text (will be wrapped as task artifact automatically)
	 */
	protected abstract String processUserMessage(String userMessage);

	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		TaskUpdater updater = new TaskUpdater(context, eventQueue);

		try {
			if (context.getTask() == null) {
				updater.submit();
			}
			updater.startWork();

			// Extract text from message in framework code
			String userMessage = extractTextFromMessage(context.getMessage());

			// Call user's method with clean string parameter
			String response = processUserMessage(userMessage);

			logger.debug("AI Response: {}", response);

			updater.addArtifact(List.of(new TextPart(response)), null, null, null);
			updater.complete();
		}
		catch (Exception e) {
			logger.error("Error executing agent task", e);
			throw new JSONRPCError(-32603, "Agent execution failed: " + e.getMessage(), null);
		}
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		logger.info("Cancelling task: {}", context.getTaskId());
		TaskUpdater updater = new TaskUpdater(context, eventQueue);
		updater.cancel();
	}

}
