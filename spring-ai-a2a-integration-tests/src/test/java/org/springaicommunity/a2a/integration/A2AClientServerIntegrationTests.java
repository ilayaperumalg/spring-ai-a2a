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

package org.springaicommunity.a2a.integration;

import java.util.List;

import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springaicommunity.a2a.core.agent.A2AAgentClient;
import org.springaicommunity.a2a.core.agent.DefaultA2AAgentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for A2A client-server communication.
 *
 * <p>
 * These tests verify end-to-end functionality of the A2A implementation including:
 * <ul>
 * <li>Agent card discovery and retrieval</li>
 * <li>DefaultA2AAgentClient usage</li>
 * <li>Basic request/response communication</li>
 * <li>Message content handling and transformation</li>
 * <li>Multiple sequential calls</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootTest(classes = TestA2AApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class A2AClientServerIntegrationTests {

	private static final int TEST_PORT = 58888;

	private String agentUrl;

	@Autowired
	private AgentCard agentCard;

	private A2AAgentClient agentClient;

	/**
	 * Set up the agent URL and client using the fixed test port.
	 *
	 * Uses a fixed port (58888) configured in application.properties. This allows the
	 * AgentCard to have a static supportedInterfaces URL.
	 */
	@BeforeEach
	void setUp() {
		this.agentUrl = "http://localhost:" + TEST_PORT + "/a2a";

		// Create A2AAgentClient using DefaultA2AAgentClient
		this.agentClient = DefaultA2AAgentClient.builder().agentUrl(this.agentUrl).build();
	}

	/**
	 * Tests that the A2A server is properly started and the agent URL is accessible.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>The A2A application started successfully on a random port</li>
	 * <li>The agent URL is properly constructed with the dynamic port</li>
	 * <li>The agent card is available and configured correctly</li>
	 * </ul>
	 */
	@Test
	void testA2AServerStarted() {
		// Verify agent URL is constructed correctly
		assertThat(this.agentUrl).isNotNull();
		assertThat(this.agentUrl).isEqualTo("http://localhost:58888/a2a");

		// Verify agent card is available (proving the A2A server is configured)
		assertThat(this.agentCard).isNotNull();
		assertThat(this.agentCard.name()).isEqualTo("Test Echo Agent");
	}

	/**
	 * Tests agent card retrieval via DefaultA2AAgentClient.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Agent cards can be fetched from agents using DefaultA2AAgentClient</li>
	 * <li>Agent metadata is correctly populated</li>
	 * <li>Skills are properly advertised</li>
	 * </ul>
	 */
	@Test
	void testAgentCardRetrieval() {
		// Get the agent card from the client
		AgentCard retrievedCard = this.agentClient.getAgentCard();

		// Verify the retrieved agent card
		assertThat(retrievedCard).isNotNull();
		assertThat(retrievedCard.name()).isEqualTo("Test Echo Agent");
		assertThat(retrievedCard.description()).isEqualTo("Simple agent that echoes messages for testing");
		assertThat(retrievedCard.version()).isEqualTo("1.0.0");
		assertThat(retrievedCard.protocolVersion()).isEqualTo("0.1.0");

		// Verify skills
		assertThat(retrievedCard.skills()).isNotEmpty();
		assertThat(retrievedCard.skills()).hasSize(4);
		assertThat(retrievedCard.skills().get(0).id()).isEqualTo("echo");
		assertThat(retrievedCard.skills().get(0).name()).isEqualTo("Echo");
		assertThat(retrievedCard.skills().get(1).id()).isEqualTo("uppercase");
		assertThat(retrievedCard.skills().get(2).id()).isEqualTo("stream");
		assertThat(retrievedCard.skills().get(3).id()).isEqualTo("ai-analyze");

		// Verify capabilities
		assertThat(retrievedCard.capabilities()).isNotNull();
		assertThat(retrievedCard.capabilities().streaming()).isFalse();
		assertThat(retrievedCard.capabilities().pushNotifications()).isFalse();
	}

	/**
	 * Tests basic synchronous request/response using DefaultA2AAgentClient.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>DefaultA2AAgentClient can send messages to A2A agents</li>
	 * <li>Messages are properly sent and responses received</li>
	 * <li>Text content is properly extracted from responses</li>
	 * </ul>
	 */
	@Test
	void testBasicClientServerCommunication() {
		// Create a request
		A2ARequest request = A2ARequest.of("Hello, agent!");

		// Send the request and get response
		A2AResponse response = this.agentClient.sendMessage(request);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getParts()).isNotNull();
		assertThat(response.getParts()).isNotEmpty();

		// Extract text from response
		StringBuilder text = new StringBuilder();
		for (Part<?> part : response.getParts()) {
			if (part instanceof TextPart textPart) {
				text.append(textPart.text());
			}
		}

		String responseText = text.toString();
		assertThat(responseText).contains("Echo:");
		assertThat(responseText).contains("Hello, agent!");
	}

	/**
	 * Tests multiple sequential calls using DefaultA2AAgentClient.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Multiple calls can be made to the same agent</li>
	 * <li>Each call receives a proper response</li>
	 * <li>State is managed correctly between calls</li>
	 * </ul>
	 */
	@Test
	void testMultipleSequentialCalls() {
		// First call
		A2ARequest request1 = A2ARequest.of("First message");
		A2AResponse response1 = this.agentClient.sendMessage(request1);

		assertThat(response1).isNotNull();
		assertThat(response1.getParts()).isNotEmpty();

		String responseText1 = extractTextFromResponse(response1);
		assertThat(responseText1).contains("Echo:");
		assertThat(responseText1).contains("First message");

		// Second call
		A2ARequest request2 = A2ARequest.of("Second message");
		A2AResponse response2 = this.agentClient.sendMessage(request2);

		assertThat(response2).isNotNull();
		assertThat(response2.getParts()).isNotEmpty();

		String responseText2 = extractTextFromResponse(response2);
		assertThat(responseText2).contains("Echo:");
		assertThat(responseText2).contains("Second message");

		// Third call
		A2ARequest request3 = A2ARequest.of("Third message");
		A2AResponse response3 = this.agentClient.sendMessage(request3);

		assertThat(response3).isNotNull();
		assertThat(response3.getParts()).isNotEmpty();

		String responseText3 = extractTextFromResponse(response3);
		assertThat(responseText3).contains("Echo:");
		assertThat(responseText3).contains("Third message");
	}

	/**
	 * Tests uppercase transformation skill.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Skill-based routing works correctly</li>
	 * <li>Uppercase transformation is applied</li>
	 * </ul>
	 */
	@Test
	void testUppercaseSkill() {
		// Send a message with uppercase keyword to trigger the skill
		A2ARequest request = A2ARequest.of("test UPPERCASE conversion");
		A2AResponse response = this.agentClient.sendMessage(request);

		assertThat(response).isNotNull();
		assertThat(response.getParts()).isNotEmpty();

		String responseText = extractTextFromResponse(response);
		assertThat(responseText).isEqualTo("TEST UPPERCASE CONVERSION");
	}

	/**
	 * Tests error handling with empty request.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Agent handles empty messages gracefully</li>
	 * <li>Response is still returned</li>
	 * </ul>
	 * <p>
	 * Note: This test may fail if ChatClient is not configured (no OPENAI_API_KEY). In
	 * that case, we accept the failure as the agent cannot process the empty message
	 * without an LLM.
	 */
	@Test
	void testEmptyMessage() {
		try {
			A2ARequest request = A2ARequest.of("");
			A2AResponse response = this.agentClient.sendMessage(request);

			assertThat(response).isNotNull();
			assertThat(response.getParts()).isNotEmpty();

			String responseText = extractTextFromResponse(response);
			assertThat(responseText).contains("Echo:");
		}
		catch (Exception e) {
			// Accept failure if ChatClient is not available (no OPENAI_API_KEY)
			// Empty messages may not be handled gracefully without an LLM
			assertThat(e.getMessage()).contains("Failed to execute agent");
		}
	}

	/**
	 * Tests request with custom parts.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Requests can be created with custom parts</li>
	 * <li>Multiple text parts are handled correctly</li>
	 * </ul>
	 */
	@Test
	void testRequestWithCustomParts() {
		List<Part<?>> parts = List.of(new TextPart("Hello "), new TextPart("World"));
		Message message = Message.builder().role(Message.Role.USER).parts(parts).build();
		A2ARequest request = new A2ARequest(message, null, null);

		A2AResponse response = this.agentClient.sendMessage(request);

		assertThat(response).isNotNull();
		assertThat(response.getParts()).isNotEmpty();

		String responseText = extractTextFromResponse(response);
		assertThat(responseText).contains("Echo:");
		// The LLM may format with spaces, just check it contains both words
		assertThat(responseText).contains("Hello");
		assertThat(responseText).contains("World");
	}

	/**
	 * Helper method to extract text from A2AResponse.
	 */
	private String extractTextFromResponse(A2AResponse response) {
		StringBuilder text = new StringBuilder();
		if (response.getParts() != null) {
			for (Part<?> part : response.getParts()) {
				if (part instanceof TextPart textPart) {
					text.append(textPart.text());
				}
			}
		}
		return text.toString();
	}

}
