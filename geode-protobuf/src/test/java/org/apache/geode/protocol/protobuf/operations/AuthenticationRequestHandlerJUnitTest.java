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
import org.apache.geode.protocol.protobuf.BasicTypes;
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
public class AuthenticationRequestHandlerJUnitTest extends OperationHandlerJUnitTest {
  @Before
  public void setup() throws Exception {
    super.setUp();

    operationHandler = new AuthenticationRequestHandler();
  }

  @Test
  public void authenticatesWithEmptyResponseFromContext() {
    when(authenticationContextStub.handleAuthenticationRequest(any())).thenReturn(new Object[0]);

    AuthenticationAPI.AuthenticationRequest
        authenticationRequest =
        AuthenticationAPI.AuthenticationRequest.newBuilder().addAuthenticationParams(BasicTypes.EncodedValue.newBuilder().setStringResult("param1")).addAuthenticationParams(BasicTypes.EncodedValue.newBuilder().setIntResult(42)).build();

    Result handlerResult = operationHandler.process(serializationServiceStub, authenticationRequest, executionContext);

    assertTrue(handlerResult instanceof Success);
    AuthenticationAPI.AuthenticationResponse authenticationResponse = (AuthenticationAPI.AuthenticationResponse)handlerResult.getMessage();
    assertEquals(0, authenticationResponse.getChallengeParamsCount());

    ArgumentCaptor<Iterable<Object>> argumentCaptor = ArgumentCaptor.forClass(Iterable.class);
    verify(authenticationContextStub, times(1)).handleAuthenticationRequest(argumentCaptor.capture());
    Iterable<Object> caughtArguments = argumentCaptor.getValue();
    Iterator<Object> iterator = caughtArguments.iterator();
    assertEquals("param1", (String)iterator.next());
    assertEquals((Integer)42, (Integer) iterator.next());
    assertFalse(iterator.hasNext());
  }
}
