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

package org.springaicommunity.a2a.core;

import java.util.List;
import java.util.stream.Collectors;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

/**
 * Represents an incoming A2A request with the message and context.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class A2ARequest {

	private final Message message;

	private final String contextId;

	private final String taskId;

	public A2ARequest(Message message, String contextId, String taskId) {
		this.message = message;
		this.contextId = contextId;
		this.taskId = taskId;
	}

	/**
	 * Gets the underlying A2A protocol message.
	 * @return the A2A message
	 */
	public Message getMessage() {
		return this.message;
	}

	/**
	 * Gets the context ID for this request.
	 * @return the context ID
	 */
	public String getContextId() {
		return this.contextId;
	}

	/**
	 * Gets the task ID for this request.
	 * @return the task ID
	 */
	public String getTaskId() {
		return this.taskId;
	}

	/**
	 * Extracts and concatenates all text content from the message parts.
	 * @return the text content
	 */
	public String getTextContent() {
		if (this.message == null || this.message.parts() == null) {
			return "";
		}
		return this.message.parts()
			.stream()
			.filter(part -> part instanceof TextPart)
			.map(part -> ((TextPart) part).text())
			.collect(Collectors.joining("\n"));
	}

	/**
	 * Gets all message parts.
	 * @return a list of message parts
	 */
	public List<Part<?>> getParts() {
		return this.message != null ? this.message.parts() : List.of();
	}

	/**
	 * Creates a simple text-based A2A request.
	 * @param text the text content
	 * @return a new A2ARequest with the text content
	 */
	public static A2ARequest of(String text) {
		TextPart textPart = new TextPart(text);
		Message message = Message.builder().role(Message.Role.USER).parts(List.of(textPart)).build();
		return new A2ARequest(message, null, null);
	}

}
