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

package org.springaicommunity.a2a.server.controller;

import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.EventKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.model.SendMessageRequest;
import org.springaicommunity.a2a.server.model.SendMessageResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for A2A message sending.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
@RestController
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

	private final RequestHandler requestHandler;

	public MessageController(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	/**
	 * Handles sendMessage JSON-RPC requests. Passes the {@code A2A-Version} request
	 * header to the {@link ServerCallContext} so that the {@link RequestHandler} can
	 * apply the correct protocol version rules (v0.3 vs v1.0).
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public SendMessageResponse sendMessage(@RequestBody SendMessageRequest request,
			@RequestHeader(value = "A2A-Version", required = false) String a2aVersion) throws A2AError {

		logger.debug("Received sendMessage request - id: {}", request.id());

		try {
			String version = (a2aVersion == null || a2aVersion.isBlank()) ? "0.3" : a2aVersion;

			// TODO: Add support for auth context, state, and extensions
			ServerCallContext context = new ServerCallContext(null, // auth context
					Map.of(), // state
					Set.of(), // extensions
					version // requested protocol version
			);

			EventKind result = this.requestHandler.onMessageSend(request.params(), context);

			logger.debug("Message processed successfully - id: {}", request.id());
			return new SendMessageResponse(request.jsonrpc(), request.id(), result);
		}
		catch (A2AError e) {
			logger.error("Error processing message - id: {}", request.id(), e);
			throw e;
		}
		catch (Exception e) {
			logger.error("Unexpected error processing message - id: {}", request.id(), e);
			throw new A2AError(-32603, "Internal error: " + e.getMessage(), null);
		}
	}

}
