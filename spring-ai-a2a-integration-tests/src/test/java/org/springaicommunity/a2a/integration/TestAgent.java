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

import org.springaicommunity.a2a.server.agentexecution.DefaultSpringAIAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Test agent used for integration testing.
 *
 * <p>
 * This agent demonstrates A2A agent implementation by extending
 * DefaultSpringAIAgentExecutor. It shows:
 * <ul>
 * <li>Extending DefaultSpringAIAgentExecutor for simplified agent implementation</li>
 * <li>Simple transformations (echo, uppercase)</li>
 * <li>Integration with ChatClient</li>
 * <li>Skill-based routing via keyword detection</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class TestAgent extends DefaultSpringAIAgentExecutor {

	public TestAgent(ChatClient chatClient) {
		super(chatClient);
	}

	@Override
	public String getSystemPrompt() {
		return """
				You are a test echo agent for integration testing.

				You support the following skills based on keywords in the user input:
				- ECHO (default): Respond with "Echo: " followed by the user's message
				- UPPERCASE: Convert the entire message to uppercase and return it
				- ANALYZE: Provide a brief analysis of the text

				Important formatting rules:
				- For ECHO: Always prefix with "Echo: " (case-sensitive)
				- For UPPERCASE: Return ONLY the uppercased text, no prefix
				- For ANALYZE: Provide a brief analysis

				Detect which skill to use based on keywords in the user input:
				- If input contains "uppercase" or "UPPERCASE" → use UPPERCASE skill
				- If input contains "analyze" or "ANALYZE" → use ANALYZE skill
				- Otherwise → use ECHO skill (default)
				""";
	}

}
