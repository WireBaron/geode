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

package org.apache.geode.internal.protocol.protobuf.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.cache.IncompatibleVersionException;
import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.internal.cache.client.protocol.ClientProtocolProcessor;
import org.apache.geode.internal.cache.tier.CommunicationMode;
import org.apache.geode.internal.protocol.MessageExecutionContext;
import org.apache.geode.internal.protocol.protobuf.ProtocolVersion;
import org.apache.geode.internal.protocol.protobuf.v1.operations.VersionValidator;
import org.apache.geode.internal.protocol.state.ConnectionStateProcessor;
import org.apache.geode.internal.protocol.state.NoSecurityConnectionStateProcessor;
import org.apache.geode.internal.protocol.statistics.ProtocolClientStatistics;

@Experimental
public final class ProtobufLocatorPipeline implements ClientProtocolProcessor {
  private final ProtocolClientStatistics statistics;
  private final InternalLocator locator;
  private final ProtobufStreamProcessor streamProcessor;
  private final ConnectionStateProcessor locatorConnectionState;
  private final VersionValidator validator;

  ProtobufLocatorPipeline(ProtobufStreamProcessor protobufStreamProcessor,
      ProtocolClientStatistics statistics, InternalLocator locator) {
    this.streamProcessor = protobufStreamProcessor;
    this.statistics = statistics;
    this.locator = locator;
    this.statistics.clientConnected();
    this.locatorConnectionState = new NoSecurityConnectionStateProcessor();
    this.validator = new VersionValidator();
  }

  @Override
  public void processMessage(InputStream inputStream, OutputStream outputStream)
      throws IOException, IncompatibleVersionException {
    handleHandshakeMessage(inputStream);
    streamProcessor.receiveMessage(inputStream, outputStream,
        new MessageExecutionContext(locator, statistics, locatorConnectionState));
  }

  @Override
  public void close() {
    this.statistics.clientDisconnected();
  }

  @Override
  public boolean socketProcessingIsFinished() {
    // All locator connections are closed after one message, so this is not used
    return false;
  }

  private void handleHandshakeMessage(InputStream inputStream) throws IOException {
    // Incoming connection had the first byte removed to determine communication mode, add that
    // back before parsing.
    PushbackInputStream handshakeStream = new PushbackInputStream(inputStream);
    handshakeStream.unread(CommunicationMode.ProtobufClientServerProtocol.getModeNumber());

    ProtocolVersion.NewConnectionClientVersion handshakeRequest =
        ProtocolVersion.NewConnectionClientVersion.parseDelimitedFrom(handshakeStream);
    int majorVersion = handshakeRequest.getMajorVersion();
    int minorVersion = handshakeRequest.getMinorVersion();
    if (!validator.isValid(majorVersion, minorVersion)) {
      throw new IOException(
          "Invalid protobuf client version number: " + majorVersion + "." + minorVersion);
    }
  }
}
