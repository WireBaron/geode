/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.internal.cache.tier.sockets.sasl.ExecutionContext;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.serialization.SerializationService;

public class AuthenticationHandshakeRequestHandler implements
    OperationHandler<AuthenticationAPI.AuthenticationHandshakeRequest, AuthenticationAPI.AuthenticationHandshakeResponse> {

  @Override
  public Result<AuthenticationAPI.AuthenticationHandshakeResponse> process(
      SerializationService serializationService,
      AuthenticationAPI.AuthenticationHandshakeRequest request, ExecutionContext executionContext) {
    String mechanism = executionContext.getAuthenticationContext().handleAuthenticationHandshake(request.getMechanismList());
    if (mechanism == null) {
      return Failure.of(BasicTypes.ErrorResponse.newBuilder().setMessage("No acceptable authentication mechanism found").build());
    }
    return Success.of(AuthenticationAPI.AuthenticationHandshakeResponse.newBuilder().setMechanism(mechanism).build());
  }
}
