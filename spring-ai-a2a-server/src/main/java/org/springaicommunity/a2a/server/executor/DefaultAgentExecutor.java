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

import java.util.List;
import java.util.stream.Collectors;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Base class for A2A AgentExecutors using Spring AI ChatClient.
 *
 * <p>
 * This executor handles Task-based execution, managing the complete task lifecycle:
 * <ul>
 * <li>Submits and starts task via {@link AgentEmitter}</li>
 * <li>Extracts user message from A2A protocol {@link Message}</li>
 * <li>Delegates to {@link ChatClientExecutorHandler} for agent-specific logic</li>
 * <li>Wraps response as task artifact and completes the task</li>
 * </ul>
 *
 * <p>
 * Implementations only need to provide the {@link ChatClientExecutorHandler} that takes a
 * Spring AI {@link ChatClient} and {@link RequestContext} and returns a String response.
 * All A2A protocol complexity and task management is handled by this base class.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
public class DefaultAgentExecutor implements AgentExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAgentExecutor.class);

	private final ChatClient chatClient;

	private final ChatClientExecutorHandler chatClientExecutorHandler;

	public DefaultAgentExecutor(ChatClient chatClient, ChatClientExecutorHandler chatClientExecutorHandler) {
		this.chatClient = chatClient;
		this.chatClientExecutorHandler = chatClientExecutorHandler;
	}

	/**
	 * Extracts text content from A2A message.
	 */
	public static String extractTextFromMessage(Message message) {
		if (message == null || message.parts() == null) {
			return "";
		}
		return message.parts()
			.stream()
			.filter(part -> part instanceof TextPart)
			.map(part -> ((TextPart) part).text())
			.collect(Collectors.joining())
			.trim();
	}

	@Override
	public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
		try {
			if (context.getTask() == null) {
				emitter.submit();
			}
			emitter.startWork();

			String response = this.chatClientExecutorHandler.execute(this.chatClient, context);

			logger.debug("AI Response: {}", response);

			emitter.addArtifact(List.of(new TextPart(response)));
			emitter.complete();
		}
		catch (Exception e) {
			logger.error("Error executing agent task", e);
			emitter.fail(new A2AError(-32603, "Agent execution failed: " + e.getMessage(), null));
		}
	}

	@Override
	public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
		logger.debug("Cancelling task: {}", context.getTaskId());
		emitter.cancel();
	}

}
