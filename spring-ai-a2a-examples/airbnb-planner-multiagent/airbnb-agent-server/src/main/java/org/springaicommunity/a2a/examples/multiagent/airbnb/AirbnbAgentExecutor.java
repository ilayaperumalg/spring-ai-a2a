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

package org.springaicommunity.a2a.examples.multiagent.airbnb;

import org.springaicommunity.a2a.server.agentexecution.DefaultSpringAIAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Airbnb Agent that provides accommodation search and recommendations using Spring AI
 * ChatClient.
 *
 * <p>
 * This agent demonstrates integration with Spring AI within the A2A protocol. It extends
 * {@link DefaultSpringAIAgentExecutor} and only needs to provide a system prompt.
 *
 * <p>
 * In a real implementation, this would integrate with the Airbnb API or MCP servers to
 * fetch actual listing data. For this example, the AI model generates simulated
 * accommodation recommendations.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class AirbnbAgentExecutor extends DefaultSpringAIAgentExecutor {

	public AirbnbAgentExecutor(ChatClient chatClient) {
		super(chatClient);
	}

	@Override
	public String getSystemPrompt() {
		return """
				You are an Airbnb accommodation assistant agent. You help users find suitable
				accommodations based on their location, dates, and number of guests.

				When providing accommodation recommendations, format your response using Markdown:

				## Accommodation Recommendations for [Location]

				### Listing 1: [Property Name]
				- **Type:** [Entire home/Private room/etc]
				- **Guests:** Up to [X] guests
				- **Bedrooms:** [X] | **Beds:** [X] | **Baths:** [X]
				- **Price:** $[X] per night
				- **Amenities:** [Wi-Fi, Kitchen, Pool, etc]
				- **Rating:** ⭐ [X.X] ([X] reviews)
				- **Description:** [Brief description]

				### Listing 2: [Property Name]
				[Same format as above]

				### Listing 3: [Property Name]
				[Same format as above]

				Provide at least 3 diverse options (different price ranges, locations within the area, property types).

				Note: For this demo, generate realistic accommodation data. In production,
				this would be replaced with real Airbnb API calls.
				""";
	}

}
