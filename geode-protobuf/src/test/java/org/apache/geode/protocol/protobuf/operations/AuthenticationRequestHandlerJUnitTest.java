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

import org.apache.geode.internal.cache.tier.sockets.sasl.Authenticator;
import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.ProtocolErrorCode;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.protocol.protobuf.utilities.ProtobufUtilities;
import org.apache.geode.serialization.exception.UnsupportedEncodingTypeException;
import org.apache.geode.test.junit.categories.UnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Category(UnitTest.class)
public class AuthenticationRequestHandlerJUnitTest extends OperationHandlerJUnitTest {
  private final String TEST_USER = "username";
  private final String TEST_PASSWORD = "password";

  @Before
  public void setUp() throws Exception {
    super.setUp();

    operationHandler = new AuthenticationRequestHandler();
  }

  @Test
  public void authenticationRequestSucceeds() throws Exception {
    when(authenticatorStub.handleAuthenticationRequest(any()))
        .thenReturn(Authenticator.AuthenticationProgress.AUTHENTICATION_SUCCEEDED);

    AuthenticationAPI.AuthenticationRequest authenticationRequest =
        AuthenticationAPI.AuthenticationRequest.newBuilder()
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_USER))
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_PASSWORD))
            .build();

    Result<AuthenticationAPI.AuthenticationResponse> authenticationResult =
        operationHandler.process(serializationServiceStub, authenticationRequest, executionContext);

    assertTrue(authenticationResult instanceof Success);
    AuthenticationAPI.AuthenticationResponse authenticationResponse =
        authenticationResult.getMessage();
    assertEquals(AuthenticationAPI.AuthenticationResult.AUTH_SUCCESS,
        authenticationResponse.getAuthenticationResult());

    ArgumentCaptor<Collection> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(authenticatorStub, times(1)).handleAuthenticationRequest(argumentCaptor.capture());
    assertEquals(1, argumentCaptor.getAllValues().size());
    assertArrayEquals(new String[] {TEST_USER, TEST_PASSWORD},
        argumentCaptor.getAllValues().get(0).toArray());
  }

  @Test
  public void encodingErrorFails() throws Exception {
    when(authenticatorStub.handleAuthenticationRequest(any()))
        .thenReturn(Authenticator.AuthenticationProgress.AUTHENTICATION_SUCCEEDED);

    BasicTypes.EncodedValue invalidPassword =
        BasicTypes.EncodedValue.newBuilder().setCustomEncodedValue(BasicTypes.CustomEncodedValue
            .newBuilder().setEncodingType(BasicTypes.EncodingType.INVALID)).build();

    when(serializationServiceStub.decode(eq(BasicTypes.EncodingType.INVALID), any()))
        .thenThrow(new UnsupportedEncodingTypeException("FAIL."));

    AuthenticationAPI.AuthenticationRequest authenticationRequest =
        AuthenticationAPI.AuthenticationRequest.newBuilder()
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_USER))
            .addAuthenticationParam(invalidPassword).build();


    Result<AuthenticationAPI.AuthenticationResponse> authenticationResult =
        operationHandler.process(serializationServiceStub, authenticationRequest, executionContext);

    assertTrue(authenticationResult instanceof Failure);
    assertEquals(ProtocolErrorCode.VALUE_ENCODING_ERROR.codeValue,
        authenticationResult.getErrorMessage().getErrorCode());
  }

  @Test
  public void authenticationRequestFails() throws Exception {
    when(authenticatorStub.handleAuthenticationRequest(any()))
        .thenReturn(Authenticator.AuthenticationProgress.AUTHENTICATION_FAILED);

    AuthenticationAPI.AuthenticationRequest authenticationRequest =
        AuthenticationAPI.AuthenticationRequest.newBuilder()
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_USER))
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_PASSWORD))
            .build();

    Result<AuthenticationAPI.AuthenticationResponse> authenticationResult =
        operationHandler.process(serializationServiceStub, authenticationRequest, executionContext);

    assertTrue(authenticationResult instanceof Success);
    AuthenticationAPI.AuthenticationResponse authenticationResponse =
        authenticationResult.getMessage();
    assertEquals(AuthenticationAPI.AuthenticationResult.AUTH_INVALID,
        authenticationResponse.getAuthenticationResult());
  }

  @Test
  public void authenticationRequestReturnsInProgress() throws Exception {
    when(authenticatorStub.handleAuthenticationRequest(any()))
        .thenReturn(Authenticator.AuthenticationProgress.AUTHENTICATION_IN_PROGRESS);

    AuthenticationAPI.AuthenticationRequest authenticationRequest =
        AuthenticationAPI.AuthenticationRequest.newBuilder()
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_USER))
            .addAuthenticationParam(
                ProtobufUtilities.createEncodedValue(serializationServiceStub, TEST_PASSWORD))
            .build();

    Result<AuthenticationAPI.AuthenticationResponse> authenticationResult =
        operationHandler.process(serializationServiceStub, authenticationRequest, executionContext);
    
    assertTrue(authenticationResult instanceof Success);
    AuthenticationAPI.AuthenticationResponse authenticationResponse =
        authenticationResult.getMessage();
    assertEquals(AuthenticationAPI.AuthenticationResult.AUTH_INPROGRESS,
        authenticationResponse.getAuthenticationResult());
  }
}
