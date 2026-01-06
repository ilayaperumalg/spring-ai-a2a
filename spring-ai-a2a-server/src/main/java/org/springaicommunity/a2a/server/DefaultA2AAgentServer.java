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

package org.springaicommunity.a2a.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springaicommunity.a2a.core.agent.A2AAgent;
import org.springaicommunity.a2a.server.agentexecution.SpringAIAgentExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-side implementation of an A2A agent that exposes A2A protocol endpoints.
 *
 * <p>
 * This server provides:
 * <ul>
 * <li>GET /.well-known/agent-card.json - Returns the agent card (RFC 8615 discovery)</li>
 * <li>GET /a2a - Returns the agent card (agent discovery)</li>
 * <li>POST /a2a - Handles A2A JSON-RPC requests</li>
 * </ul>
 *
 * <p>
 * The server implements the {@link A2AAgent} interface, allowing it to be used both as a
 * local agent (via direct method calls) and as a remote agent (via HTTP endpoints).
 *
 * <p>
 * The server delegates task execution to {@link SpringAIAgentExecutor} and manages the
 * protocol-level concerns like JSON-RPC formatting and task state management.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@RestController
public class DefaultA2AAgentServer implements A2AAgentServer {

	private final AgentCard agentCard;

	private final SpringAIAgentExecutor agentExecutor;

	private final Map<String, Task> taskStore = new HashMap<>();

	/**
	 * Create a new DefaultA2AAgentServer with a SpringAIAgentExecutor.
	 * <p>
	 * This constructor accepts Spring AI's {@link SpringAIAgentExecutor} which combines
	 * both the A2A SDK's AgentExecutor and Spring AI's AgentExecutorLifecycle interfaces.
	 * <p>
	 * <strong>Example:</strong>
	 *
	 * <pre>
	 * SpringAIAgentExecutor weatherAgent = new WeatherAgentExecutor(chatClient);
	 * DefaultA2AAgentServer server = new DefaultA2AAgentServer(agentCard, weatherAgent);
	 * </pre>
	 * @param agentCard the agent card describing the agent's capabilities
	 * @param agentExecutor the Spring AI agent executor
	 */
	public DefaultA2AAgentServer(AgentCard agentCard, SpringAIAgentExecutor agentExecutor) {
		this.agentCard = agentCard;
		this.agentExecutor = agentExecutor;
	}

	/**
	 * Get the agent card.
	 * @return the agent card
	 */
	@Override
	public AgentCard getAgentCard() {
		return this.agentCard;
	}

	/**
	 * Returns the agent card via the RFC 8615 well-known URI discovery endpoint.
	 * @return the agent card as JSON
	 */
	@GetMapping(path = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Override
	public AgentCard getWellKnownAgentCard() {
		return this.agentCard;
	}

	/**
	 * Returns the agent card via the base A2A endpoint.
	 * <p>
	 * This provides an alternative way to retrieve the agent card beyond the well-known
	 * URI.
	 * @return the agent card as JSON
	 */
	@GetMapping(path = "${spring.ai.a2a.server.base-path:/a2a}", produces = MediaType.APPLICATION_JSON_VALUE)
	public AgentCard getAgentCardViaBaseEndpoint() {
		return this.agentCard;
	}

	/**
	 * Handles A2A JSON-RPC requests.
	 *
	 * <p>
	 * This simplified implementation handles basic message exchange. A full
	 * implementation would need to:
	 * <ul>
	 * <li>Support all 11 A2A protocol operations</li>
	 * <li>Implement complete JSON-RPC 2.0 specification</li>
	 * <li>Handle batch requests</li>
	 * <li>Manage task lifecycle properly</li>
	 * <li>Support streaming via Server-Sent Events</li>
	 * </ul>
	 * @param requestBody the JSON-RPC request
	 * @return the JSON-RPC response
	 */
	@PostMapping(path = "${spring.ai.a2a.server.base-path:/a2a}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Override
	public Map<String, Object> handleJsonRpcRequest(@RequestBody Map<String, Object> requestBody) {
		try {
			String method = (String) requestBody.get("method");
			Object id = requestBody.get("id");
			Map<String, Object> params = (Map<String, Object>) requestBody.get("params");

			// Normalize method name to lowercase for case-insensitive comparison
			String normalizedMethod = method != null ? method.toLowerCase() : "";

			if ("submittask".equals(normalizedMethod)) {
				return handleSubmitTask(params, id);
			}
			else if ("sendmessage".equals(normalizedMethod)) {
				return handleSendMessage(params, id);
			}
			else if ("gettask".equals(normalizedMethod)) {
				return handleGetTask(params, id);
			}
			else {
				return createErrorResponse(id, -32601, "Method not found: " + method);
			}
		}
		catch (Exception e) {
			return createErrorResponse(requestBody.get("id"), -32603, "Internal error: " + e.getMessage());
		}
	}

	/**
	 * Handles submitTask JSON-RPC method.
	 */
	private Map<String, Object> handleSubmitTask(Map<String, Object> params, Object id) {
		try {
			// Extract or generate contextId
			String contextId = (String) params.get("contextId");
			if (contextId == null || contextId.isEmpty()) {
				contextId = UUID.randomUUID().toString();
			}

			// Create a new task
			String taskId = UUID.randomUUID().toString();
			Task task = Task.builder()
				.contextId(contextId)
				.id(taskId)
				.status(new TaskStatus(TaskState.SUBMITTED))
				.build();

			this.taskStore.put(taskId, task);

			Map<String, Object> result = new HashMap<>();
			result.put("taskId", taskId);

			return this.createSuccessResponse(id, result);
		}
		catch (Exception e) {
			return this.createErrorResponse(id, -32603, "Error submitting task: " + e.getMessage());
		}
	}

	/**
	 * Handles sendMessage JSON-RPC method.
	 */
	private Map<String, Object> handleSendMessage(Map<String, Object> params, Object id) {
		try {
			String taskId = (String) params.get("taskId");
			Map<String, Object> messageMap = (Map<String, Object>) params.get("message");

			// Parse message
			Message message = this.parseMessage(messageMap);

			// Extract or generate contextId from message
			String contextId = message.contextId();
			if (contextId == null || contextId.isEmpty()) {
				contextId = UUID.randomUUID().toString();
			}

			// If no taskId provided, create one automatically
			if (taskId == null || taskId.isEmpty()) {
				taskId = UUID.randomUUID().toString();
				Task task = Task.builder()
					.contextId(contextId)
					.id(taskId)
					.status(new TaskStatus(TaskState.SUBMITTED))
					.build();
				this.taskStore.put(taskId, task);
			}

			// Update task state to WORKING
			Task task = this.taskStore.get(taskId);
			if (task != null) {
				task = Task.builder().contextId(contextId).id(taskId).status(new TaskStatus(TaskState.WORKING)).build();
				this.taskStore.put(taskId, task);
			}

			// Create A2ARequest (message, contextId, taskId)
			A2ARequest request = new A2ARequest(message, contextId, taskId);

			// Execute using the Spring AI agent executor (synchronous execution)
			A2AResponse response = this.agentExecutor.executeSynchronous(request);

			// Update task state to COMPLETED
			task = Task.builder().contextId(contextId).id(taskId).status(new TaskStatus(TaskState.COMPLETED)).build();
			this.taskStore.put(taskId, task);

			// Create response message
			Message responseMessage = Message.builder().role(Message.Role.AGENT).parts(response.getParts()).build();

			Map<String, Object> result = new HashMap<>();
			Map<String, Object> serializedMessage = this.serializeMessage(responseMessage);
			result.put("message", serializedMessage);
			// Don't include taskId in response - not part of SendMessageResponse schema

			return this.createSuccessResponse(id, result);
		}
		catch (Exception e) {
			return this.createErrorResponse(id, -32603, "Error sending message: " + e.getMessage());
		}
	}

	/**
	 * Handles getTask JSON-RPC method.
	 */
	private Map<String, Object> handleGetTask(Map<String, Object> params, Object id) {
		try {
			String taskId = (String) params.get("taskId");
			Task task = this.taskStore.get(taskId);

			if (task == null) {
				return this.createErrorResponse(id, -32602, "Task not found: " + taskId);
			}

			Map<String, Object> result = new HashMap<>();
			result.put("task", task);

			return this.createSuccessResponse(id, result);
		}
		catch (Exception e) {
			return this.createErrorResponse(id, -32603, "Error getting task: " + e.getMessage());
		}
	}

	/**
	 * Parses a message from a map.
	 */
	private Message parseMessage(Map<String, Object> messageMap) {
		String roleStr = (String) messageMap.get("role");

		// Handle various role name formats:
		// - "user" or "USER" (correct format)
		// - "agent" or "AGENT" (correct format)
		// - "ROLE_USER" (A2A SDK sends with ROLE_ prefix)
		// - "ROLE_AGENT" (A2A SDK sends with ROLE_ prefix)
		String normalizedRole = roleStr.toUpperCase();
		if (normalizedRole.startsWith("ROLE_")) {
			normalizedRole = normalizedRole.substring(5); // Strip "ROLE_" prefix
		}

		Message.Role role = Message.Role.valueOf(normalizedRole);

		List<Map<String, Object>> partsList = (List<Map<String, Object>>) messageMap.get("parts");
		List<Part<?>> parts = partsList.stream().<Part<?>>map(partMap -> {
			String type = (String) partMap.get("type");
			if ("text".equals(type) || type == null) {
				// If type is null, assume it's a text part
				String text = (String) partMap.get("text");
				if (text != null) {
					return new TextPart(text);
				}
			}
			// Add support for other part types as needed
			throw new IllegalArgumentException("Unsupported part type: " + type);
		}).toList();

		return Message.builder().role(role).parts(parts).build();
	}

	/**
	 * Serializes a message to a map for JSON-RPC response.
	 */
	private Map<String, Object> serializeMessage(Message message) {
		Map<String, Object> messageMap = new HashMap<>();

		// Generate messageId for the response
		String messageId = UUID.randomUUID().toString();
		messageMap.put("messageId", messageId);

		// Use protobuf-style role name (ROLE_AGENT, ROLE_USER)
		messageMap.put("role", "ROLE_" + message.role().name());

		List<Map<String, Object>> partsList = new ArrayList<>();
		if (message.parts() != null) {
			for (Part<?> part : message.parts()) {
				Map<String, Object> partMap = new HashMap<>();
				if (part instanceof TextPart textPart) {
					// Don't include "type" field - A2A SDK infers type from content
					partMap.put("text", textPart.text());
				}
				// Add support for other part types as needed
				partsList.add(partMap);
			}
		}
		messageMap.put("parts", partsList);

		return messageMap;
	}

	/**
	 * Creates a JSON-RPC success response.
	 */
	private Map<String, Object> createSuccessResponse(Object id, Object result) {
		Map<String, Object> response = new HashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("result", result);
		return response;
	}

	/**
	 * Creates a JSON-RPC error response.
	 */
	private Map<String, Object> createErrorResponse(Object id, int code, String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("code", code);
		error.put("message", message);

		Map<String, Object> response = new HashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("error", error);
		return response;
	}

}
