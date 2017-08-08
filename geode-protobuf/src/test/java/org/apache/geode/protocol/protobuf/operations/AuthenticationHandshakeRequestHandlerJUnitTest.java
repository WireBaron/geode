package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.ProtocolErrorCode;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.test.junit.categories.UnitTest;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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
  public void respondsWithAuthenticationHandshakeResponseContainingAgreedUponMechanism() {
    when(authenticatorStub.handleHandshakeRequest(any())).thenReturn(Optional.of("PLAIN"));
    AuthenticationAPI.AuthenticationHandshakeRequest
        clientHandshakeRequest =
        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism("PLAIN").addMechanism("UnknownMechanismToServer").build();

    Success<AuthenticationAPI.AuthenticationHandshakeResponse> result =
        (Success<AuthenticationAPI.AuthenticationHandshakeResponse>) operationHandler
            .process(serializationServiceStub, clientHandshakeRequest, executionContext);

    assertEquals("PLAIN", result.getMessage().getMechanism());
    ArgumentCaptor<Collection> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(authenticatorStub, times(1)).handleHandshakeRequest(argumentCaptor.capture());
    assertEquals(1, argumentCaptor.getAllValues().size());
    assertArrayEquals(new String[]{"PLAIN", "UnknownMechanismToServer"}, argumentCaptor.getAllValues().get(0).toArray());
  }

  @Test
  public void submitUnsupportedAuthenticationMechanism() {
    when(authenticatorStub.handleHandshakeRequest(any())).thenReturn(Optional.empty());
    String unknown_mechanism = "UnknownMechanismToServer";
    AuthenticationAPI.AuthenticationHandshakeRequest
        handshakeRequest =
        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism(
            unknown_mechanism).build();

    Result result =
        operationHandler.process(serializationServiceStub, handshakeRequest, executionContext);

    assertThat(result, new IsInstanceOf(Failure.class));
    assertEquals(ProtocolErrorCode.AUTHENTICATION_FAILED.codeValue,
        result.getErrorMessage().getErrorCode());
    verify(authenticatorStub, times(1)).handleHandshakeRequest(any());
  }
}
