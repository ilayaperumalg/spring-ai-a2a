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

package org.springaicommunity.a2a.examples.multiagent.host;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for the Host Agent.
 *
 * <p>
 * This controller provides a simple HTTP interface for interacting with the multi-agent
 * travel planning system. Users can submit queries via POST requests and receive
 * responses that are orchestrated across multiple specialized agents.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api")
public class HostAgentController {

	private final HostAgentOrchestrator orchestrator;

	public HostAgentController(HostAgentOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	/**
	 * Handle user queries.
	 * @param request the request containing the user message
	 * @return the response from the orchestrated agents
	 */
	@PostMapping("/query")
	public ResponseEntity<Map<String, String>> handleQuery(@RequestBody Map<String, String> request) {
		String userMessage = request.get("message");

		if (userMessage == null || userMessage.trim().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "Message is required", "response", "Please provide a message to process."));
		}

		try {
			String response = this.orchestrator.processRequest(userMessage);
			return ResponseEntity.ok(Map.of("message", userMessage, "response", response));
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Error processing request", "details", e.getMessage()));
		}
	}

	/**
	 * Health check endpoint.
	 * @return health status
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of("status", "UP", "service", "Host Agent Orchestrator"));
	}

}
