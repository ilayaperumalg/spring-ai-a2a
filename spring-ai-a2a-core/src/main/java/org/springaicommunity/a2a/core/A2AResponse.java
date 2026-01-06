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
import java.util.Map;

import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

/**
 * Represents an A2A response that can be sent to other agents.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public final class A2AResponse {

	private final List<Part<?>> parts;

	private final Map<String, Object> metadata;

	private A2AResponse(List<Part<?>> parts, Map<String, Object> metadata) {
		this.parts = parts;
		this.metadata = metadata;
	}

	/**
	 * Creates a simple text response.
	 * @param text the response text
	 * @return an A2A response containing the text
	 */
	public static A2AResponse text(String text) {
		return new A2AResponse(List.of(new TextPart(text)), Map.of());
	}

	/**
	 * Creates a response with multiple parts.
	 * @param parts the response parts
	 * @return an A2A response containing the parts
	 */
	public static A2AResponse of(List<Part<?>> parts) {
		return new A2AResponse(parts, Map.of());
	}

	/**
	 * Creates a response with parts and metadata.
	 * @param parts the response parts
	 * @param metadata the metadata
	 * @return an A2A response
	 */
	public static A2AResponse of(List<Part<?>> parts, Map<String, Object> metadata) {
		return new A2AResponse(parts, metadata);
	}

	/**
	 * Gets the response parts.
	 * @return a list of parts
	 */
	public List<Part<?>> getParts() {
		return this.parts;
	}

	/**
	 * Gets the response metadata.
	 * @return metadata map
	 */
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	/**
	 * Extracts text content from all text parts.
	 * @return concatenated text content
	 */
	public String getTextContent() {
		StringBuilder sb = new StringBuilder();
		for (Part<?> part : this.parts) {
			if (part instanceof TextPart textPart) {
				sb.append(textPart.text());
			}
		}
		return sb.toString();
	}

}
