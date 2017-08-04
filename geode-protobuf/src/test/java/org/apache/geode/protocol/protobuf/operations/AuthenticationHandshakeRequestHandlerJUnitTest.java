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

import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.test.junit.categories.UnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(UnitTest.class)
public class AuthenticationHandshakeRequestHandlerJUnitTest extends OperationHandlerJUnitTest {
  @Before
  public void setup() throws Exception {
    super.setUp();

    operationHandler = new AuthenticationHandshakeRequestHandler();
  }

  @Test
  public void respondsWithAuthenticationHandshakeResponse() {
    when(authenticationContextStub.handleAuthenticationHandshake(any())).thenReturn("PLAIN");

    AuthenticationAPI.AuthenticationHandshakeRequest
        handshakeRequest =
        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism("PLAIN").build();

    Result handlerResult = operationHandler.process(serializationServiceStub, handshakeRequest, executionContext);

    ArgumentCaptor<Iterable<String>> mechanismCaptor = ArgumentCaptor.forClass(Iterable.class);
    assertTrue(handlerResult instanceof Success);
    AuthenticationAPI.AuthenticationHandshakeResponse authenticationHandshakeResponse = (AuthenticationAPI.AuthenticationHandshakeResponse)handlerResult.getMessage();

    assertEquals("PLAIN", authenticationHandshakeResponse.getMechanism());
    verify(authenticationContextStub, times(1)).handleAuthenticationHandshake(mechanismCaptor.capture());
    Iterable<String> caughtMechanisms = mechanismCaptor.getValue();
    Iterator<String> iterator = caughtMechanisms.iterator();
    assertEquals("PLAIN", iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void respondsWithAErrorWhenContextCantFindMechanism() {
    when(authenticationContextStub.handleAuthenticationHandshake(any())).thenReturn(null);

    AuthenticationAPI.AuthenticationHandshakeRequest
        handshakeRequest =
        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism("PLAIN").build();

    Result handlerResult = operationHandler.process(serializationServiceStub, handshakeRequest, executionContext);
    assertTrue(handlerResult instanceof Failure);
  }
}
