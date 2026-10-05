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

package org.springaicommunity.a2a.server.model;

import org.a2aproject.sdk.spec.EventKind;

/**
 * JSON-RPC 2.0 envelope for A2A sendMessage responses.
 *
 * <p>
 * The A2A Java SDK 1.4.0 removed the transport envelope types from the spec module. This
 * record provides the JSON-RPC 2.0 framing for Spring MVC controllers.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.4.0
 */
public record SendMessageResponse(String jsonrpc, String id, EventKind result) {

}
