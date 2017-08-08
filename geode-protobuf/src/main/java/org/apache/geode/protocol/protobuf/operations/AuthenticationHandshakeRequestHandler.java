package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.internal.cache.tier.sockets.sasl.ExecutionContext;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.ProtocolErrorCode;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.serialization.SerializationService;

import java.util.Optional;

public class AuthenticationHandshakeRequestHandler implements
    OperationHandler<AuthenticationAPI.AuthenticationHandshakeRequest, AuthenticationAPI.AuthenticationHandshakeResponse> {

  @Override
  public Result<AuthenticationAPI.AuthenticationHandshakeResponse> process(
      SerializationService serializationService,
      AuthenticationAPI.AuthenticationHandshakeRequest request, ExecutionContext executionContext) {
    Optional<String> mechanism = executionContext.getAuthenticator().handleHandshakeRequest(request.getMechanismList());
    if (mechanism.isPresent()) {
      return Success.of(AuthenticationAPI.AuthenticationHandshakeResponse.newBuilder().setMechanism(mechanism.get())
          .build());
    } else {
      return Failure.of(BasicTypes.ErrorResponse.newBuilder().setErrorCode(ProtocolErrorCode.AUTHENTICATION_FAILED.codeValue).setMessage("No mutually agreed upon mechanism").build());
    }
  }
}
