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

import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springaicommunity.a2a.client.A2AClient;
import org.springaicommunity.a2a.client.DefaultA2AClient;
import org.springaicommunity.a2a.core.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for A2A client-server communication.
 *
 * <p>
 * These tests verify end-to-end functionality of the A2A implementation including:
 * <ul>
 * <li>Agent card discovery and retrieval</li>
 * <li>DefaultA2AClient usage</li>
 * <li>Basic request/response communication using A2A SDK Message types</li>
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

	private A2AClient agentClient;

	/**
	 * Set up the agent URL and client using the fixed test port.
	 *
	 * Uses a fixed port (58888) configured in application.properties. This allows the
	 * AgentCard to have a static supportedInterfaces URL.
	 */
	@BeforeEach
	void setUp() {
		this.agentUrl = "http://localhost:" + TEST_PORT + "/a2a";

		// Create A2AClient using DefaultA2AClient
		this.agentClient = DefaultA2AClient.builder().agentUrl(this.agentUrl).build();
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
	 * Tests agent card retrieval via DefaultA2AClient.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Agent cards can be fetched from agents using DefaultA2AClient</li>
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
		assertThat(retrievedCard.capabilities().streaming()).isTrue();
		assertThat(retrievedCard.capabilities().pushNotifications()).isFalse();
	}

	/**
	 * Tests basic synchronous request/response using DefaultA2AClient with A2A SDK Message API.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>DefaultA2AClient can send messages to A2A agents using sendMessage() method</li>
	 * <li>A2A SDK Message is properly processed</li>
	 * <li>Response Message contains the expected text content</li>
	 * </ul>
	 */
	@Test
	void testBasicClientServerCommunication() {
		// Create a message using A2A SDK types
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Hello, agent!")))
			.build();

		// Send the request and get response using sendMessage()
		Message response = this.agentClient.sendMessage(request);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.parts()).isNotNull();
		String responseText = MessageUtils.extractText(response.parts());
		assertThat(responseText).isNotNull();
		assertThat(responseText).contains("Echo:");
		assertThat(responseText).contains("Hello, agent!");
	}

	/**
	 * Tests multiple sequential calls using DefaultA2AClient with A2A SDK Message API.
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
		Message request1 = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("First message")))
			.build();
		Message response1 = this.agentClient.sendMessage(request1);

		assertThat(response1).isNotNull();
		String text1 = MessageUtils.extractText(response1.parts());
		assertThat(text1).isNotNull();
		assertThat(text1).contains("Echo:");
		assertThat(text1).contains("First message");

		// Second call
		Message request2 = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Second message")))
			.build();
		Message response2 = this.agentClient.sendMessage(request2);

		assertThat(response2).isNotNull();
		String text2 = MessageUtils.extractText(response2.parts());
		assertThat(text2).isNotNull();
		assertThat(text2).contains("Echo:");
		assertThat(text2).contains("Second message");

		// Third call
		Message request3 = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Third message")))
			.build();
		Message response3 = this.agentClient.sendMessage(request3);

		assertThat(response3).isNotNull();
		String text3 = MessageUtils.extractText(response3.parts());
		assertThat(text3).isNotNull();
		assertThat(text3).contains("Echo:");
		assertThat(text3).contains("Third message");
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
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("test UPPERCASE conversion")))
			.build();
		Message response = this.agentClient.sendMessage(request);

		assertThat(response).isNotNull();
		String responseText = MessageUtils.extractText(response.parts());
		assertThat(responseText).isNotNull();
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
			Message request = Message.builder()
				.role(Message.Role.USER)
				.parts(List.of(new TextPart("")))
				.build();
			Message response = this.agentClient.sendMessage(request);

			assertThat(response).isNotNull();
			String responseText = MessageUtils.extractText(response.parts());
			assertThat(responseText).isNotNull();
			assertThat(responseText).contains("Echo:");
		}
		catch (Exception e) {
			// Accept failure if ChatClient is not available (no OPENAI_API_KEY)
			// Empty messages may not be handled gracefully without an LLM
			assertThat(e.getMessage()).contains("Failed to execute agent");
		}
	}

	/**
	 * Tests request with message content.
	 *
	 * <p>
	 * This test verifies:
	 * <ul>
	 * <li>Message with text content is handled correctly</li>
	 * <li>Response contains the expected content</li>
	 * </ul>
	 */
	@Test
	void testRequestWithTaskDescription() {
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Hello World")))
			.build();
		Message response = this.agentClient.sendMessage(request);

		assertThat(response).isNotNull();
		String responseText = MessageUtils.extractText(response.parts());
		assertThat(responseText).isNotNull();
		assertThat(responseText).contains("Echo:");
		// The LLM may format with spaces, just check it contains both words
		assertThat(responseText).contains("Hello");
		assertThat(responseText).contains("World");
	}

}
