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

package org.springaicommunity.a2a.examples.multiagent.weather;

import org.springaicommunity.a2a.server.agentexecution.DefaultSpringAIAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Weather Agent that provides weather information using Spring AI ChatClient.
 *
 * <p>
 * This agent demonstrates integration with Spring AI within the A2A protocol. It extends
 * {@link DefaultSpringAIAgentExecutor} and only needs to provide a system prompt.
 *
 * <p>
 * In a real implementation, this would integrate with actual weather APIs or MCP servers
 * to fetch real-time weather data. For this example, the AI model generates simulated
 * weather information.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class WeatherAgentExecutor extends DefaultSpringAIAgentExecutor {

	public WeatherAgentExecutor(ChatClient chatClient) {
		super(chatClient);
	}

	@Override
	public String getSystemPrompt() {
		return """
				You are a weather assistant agent. You provide accurate weather information
				for locations. When asked about weather, provide temperature, conditions,
				humidity, and wind speed in a clear, concise format using Markdown.

				Format your response like this:
				## Weather for [Location]
				**Date:** [Date]
				**Temperature:** [Temp]°F / [Temp]°C
				**Conditions:** [Sunny/Cloudy/Rainy/etc]
				**Humidity:** [X]%
				**Wind Speed:** [X] mph

				Note: For this demo, generate realistic weather data. In production,
				this would be replaced with real API calls.
				""";
	}

}
