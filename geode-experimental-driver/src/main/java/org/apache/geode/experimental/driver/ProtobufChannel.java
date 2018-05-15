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
package org.apache.geode.experimental.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Set;

import org.apache.geode.internal.protocol.protobuf.v1.ClientProtocol;
import org.apache.geode.internal.protocol.protobuf.v1.ClientProtocol.ErrorResponse;
import org.apache.geode.internal.protocol.protobuf.v1.ClientProtocol.Message;
import org.apache.geode.internal.protocol.protobuf.v1.ClientProtocol.Message.MessageTypeCase;

class ProtobufChannel {
  ProtobufConnectionPool connectionPool;
  boolean closed = false;

  public ProtobufChannel(final Set<InetSocketAddress> locators, String username, String password,
      String keyStorePath, String trustStorePath, String protocols, String ciphers,
      ValueSerializer serializer, int maxServerConnections, int minServerConnections,
      int serverConnectionReapingInterval) throws GeneralSecurityException, IOException {
    connectionPool = new ProtobufConnectionPool(minServerConnections, maxServerConnections,
        serverConnectionReapingInterval, locators, username, password, keyStorePath, trustStorePath,
        protocols, ciphers, serializer);
  }

  public void close() throws IOException {
    connectionPool.closeAllConnections();
    closed = true;
  }

  public boolean isClosed() {
    return closed;
  }

  Message sendRequest(final Message request, MessageTypeCase expectedResult) throws IOException {
    Socket socket = null;
    try {
      socket = connectionPool.getConnection();
      final OutputStream output = socket.getOutputStream();
      request.writeDelimitedTo(output);
      output.flush();
      Message response = readResponse(socket);

      if (!response.getMessageTypeCase().equals(expectedResult)) {
        throw new RuntimeException(
            "Got invalid response for request " + request + ", response " + response);
      }
      return response;
    } catch (GeneralSecurityException | InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      if (socket != null) {
        connectionPool.release(socket);
      }
    }
  }

  private Message readResponse(Socket socket) throws IOException {
    final InputStream inputStream = socket.getInputStream();
    Message response = ClientProtocol.Message.parseDelimitedFrom(inputStream);
    if (response == null) {
      throw new IOException("Unable to parse a response message due to EOF");
    }
    final ErrorResponse errorResponse = response.getErrorResponse();
    if (errorResponse != null && errorResponse.hasError()) {
      throw new IOException(errorResponse.getError().getMessage());
    }
    return response;
  }
}
