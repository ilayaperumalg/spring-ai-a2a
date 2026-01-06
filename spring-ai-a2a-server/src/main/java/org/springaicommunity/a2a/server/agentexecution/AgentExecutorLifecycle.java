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

import java.util.List;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Part;

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;

/**
 * Lifecycle interface for A2A agent executors with simplified hooks.
 *
 * <p>
 * This interface provides simplified lifecycle methods for agent execution. Instead of
 * implementing the full A2A Java SDK {@link io.a2a.server.agentexecution.AgentExecutor}
 * interface, implementations only need to focus on the core agent logic.
 *
 * <p>
 * Lifecycle hooks:
 * <ul>
 * <li>{@link #onExecute(String, RequestContext, TaskUpdater)} - Process the user request
 * and return response content</li>
 * <li>{@link #onComplete(RequestContext, TaskUpdater)} - Perform cleanup or
 * post-processing after successful completion</li>
 * <li>{@link #onCancel(RequestContext, TaskUpdater)} - Handle cancellation requests</li>
 * <li>{@link #onError(Exception, RequestContext, TaskUpdater)} - Handle execution
 * errors</li>
 * </ul>
 *
 * <p>
 * This interface is typically implemented by {@link DefaultSpringAIAgentExecutor} which
 * provides the bridge to the A2A Java SDK AgentExecutor interface.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see DefaultSpringAIAgentExecutor
 */
public interface AgentExecutorLifecycle {

	/**
	 * Execute the agent logic and return response parts.
	 * <p>
	 * This method is called after the task has been submitted and started.
	 * Implementations should process the user input and return the response content as a
	 * list of parts.
	 * @param userInput the user's input extracted from the request
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 * @return a list of response parts (e.g., TextPart, ImagePart)
	 * @throws Exception if execution fails
	 */
	List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) throws Exception;

	/**
	 * Perform cleanup or post-processing after successful task completion.
	 * <p>
	 * This method is called after {@link #onExecute(String, RequestContext, TaskUpdater)}
	 * completes successfully and artifacts have been added to the task, but before the
	 * task is marked as completed. Override this method to perform any cleanup, logging,
	 * metrics collection, or post-processing.
	 * <p>
	 * Default implementation does nothing. This is an optional lifecycle hook.
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 * @throws Exception if post-processing fails
	 */
	default void onComplete(RequestContext context, TaskUpdater taskUpdater) throws Exception {
		// Default implementation: no-op
	}

	/**
	 * Handle task cancellation.
	 * <p>
	 * Default implementation calls {@link TaskUpdater#cancel()}. Override this method to
	 * perform cleanup or custom cancellation logic.
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 * @throws JSONRPCError if cancellation fails
	 */
	default void onCancel(RequestContext context, TaskUpdater taskUpdater) throws JSONRPCError {
		taskUpdater.cancel();
	}

	/**
	 * Handle execution errors.
	 * <p>
	 * Default implementation calls {@link TaskUpdater#fail()}. Override this method to
	 * provide custom error handling or error reporting.
	 * @param error the exception that occurred
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 */
	default void onError(Exception error, RequestContext context, TaskUpdater taskUpdater) {
		taskUpdater.fail();
	}

	/**
	 * Executes the agent synchronously and returns the response.
	 * <p>
	 * This method provides synchronous execution support for agents. The default
	 * implementation is provided by {@link DefaultSpringAIAgentExecutor}.
	 * @param request the A2A request
	 * @return the A2A response
	 */
	A2AResponse executeSynchronous(A2ARequest request);

}
