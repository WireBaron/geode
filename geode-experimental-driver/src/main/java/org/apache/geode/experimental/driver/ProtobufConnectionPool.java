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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import org.apache.geode.internal.protocol.protobuf.ProtocolVersion;
import org.apache.geode.internal.protocol.protobuf.v1.BasicTypes;
import org.apache.geode.internal.protocol.protobuf.v1.ClientProtocol;
import org.apache.geode.internal.protocol.protobuf.v1.ConnectionAPI;
import org.apache.geode.internal.protocol.protobuf.v1.LocatorAPI;

// This class contains a collection of sockets to a geode server for sending and receiving protobuf
// messages. It operates as follows:
// - When a new request comes in hand it the first available socket. Note that the sockets are
// ordered so that we will prefer reusing recently used sockets over spreading requests over as many
// connections as possible.
// - If a request comes in and all open sockets are busy, but there are less than 'maxConnections'
// sockets, open a new socket and use it for the new request
// - If there are more than 'minConnections' sockets open, and at least one socket has been idle for
// 'idleSocketReapingThreshold', close the last socket. Repeat this every
// 'idleSocketReapingThreshold' seconds while there are idle sockets, and more than 'minConnections'
// sockets.
public class ProtobufConnectionPool {
  // Under pressure open up to this many connections
  private int maxConnections;

  // Don't close any sockets due to lack of use if there are this or fewer
  private int minConnections;

  // While there are more than 'minConnections', close idle connections at this interval
  private int idleSocketReapingThreshold;

  // Data for establishing connections
  final Set<InetSocketAddress> locators;
  final String username;
  final String password;
  final String keyStorePath;
  final String trustStorePath;
  final String protocols;
  final String ciphers;
  final ValueSerializer serializer;

  private class ProtobufConnection {
    Socket socket;
    boolean inUse;

    public ProtobufConnection() throws GeneralSecurityException, IOException {
      socket = connectToAServer(locators, username, password, keyStorePath, trustStorePath,
          protocols, ciphers);
      inUse = false;
    }
  }

  private ArrayList<ProtobufConnection> connections;
  private HashMap<Socket, Integer> socketToConnectionId;

  public ProtobufConnectionPool(int minConnections, int maxConnections,
      int idleSocketReapingThreshold, final Set<InetSocketAddress> locators, String username,
      String password, String keyStorePath, String trustStorePath, String protocols, String ciphers,
      ValueSerializer serializer) throws GeneralSecurityException, IOException {
    this.minConnections = minConnections;
    this.maxConnections = maxConnections;
    this.idleSocketReapingThreshold = idleSocketReapingThreshold;

    this.locators = locators;
    this.username = username;
    this.password = password;
    this.keyStorePath = keyStorePath;
    this.trustStorePath = trustStorePath;
    this.protocols = protocols;
    this.ciphers = ciphers;
    this.serializer = serializer;

    connections = new ArrayList<>(maxConnections);
    socketToConnectionId = new HashMap<>(maxConnections);

    ArrayList<Socket> initialConnections = new ArrayList<>(minConnections);
    try {
      for (int i = 0; i < minConnections; ++i) {
        initialConnections.add(getConnectionNonBlocking());
      }
    } catch (GeneralSecurityException | IOException e) {
      throw e;
    } finally {
      for (Socket s : initialConnections) {
        release(s);
      }
    }
  }

  public synchronized boolean connectionsAvailable() {
    if (connections.size() < maxConnections) {
      return true;
    }
    for (ProtobufConnection connection : connections) {
      if (!connection.inUse) {
        return true;
      }
    }
    return false;
  }

  // This will return a socket, possibly blocking until one is available
  public synchronized Socket getConnection()
      throws GeneralSecurityException, IOException, InterruptedException {
    Socket result = getConnectionNonBlocking();
    while (result == null) {
      wait();
      result = getConnectionNonBlocking();
    }
    return result;
  }

  // This will return a socket if one is available, else returns null
  public synchronized Socket getConnectionNonBlocking()
      throws GeneralSecurityException, IOException {
    for (ProtobufConnection connection : connections) {
      if (!connection.inUse) {
        connection.inUse = true;
        return connection.socket;
      }
    }
    if (connections.size() < maxConnections) {
      ProtobufConnection newConnection = new ProtobufConnection();
      int newIndex = connections.size();
      connections.add(newConnection);
      socketToConnectionId.put(newConnection.socket, newIndex);
      newConnection.inUse = true;
      return newConnection.socket;
    }
    return null;
  }

  public synchronized void release(Socket socket) {
    connections.get(socketToConnectionId.get(socket)).inUse = false;
    notify();
  }

  public void closeAllConnections() throws IOException {
    // TODO: check for in use
    for (ProtobufConnection connection : connections) {
      connection.socket.close();
    }
  }


  private Socket connectToAServer(final Set<InetSocketAddress> locators, String username,
      String password, String keyStorePath, String trustStorePath, String protocols, String ciphers)
      throws GeneralSecurityException, IOException {
    InetSocketAddress server =
        findAServer(locators, username, password, keyStorePath, trustStorePath, protocols, ciphers);
    Socket socket = createSocket(server.getAddress(), server.getPort(), keyStorePath,
        trustStorePath, protocols, ciphers);
    socket.setTcpNoDelay(true);
    socket.setSendBufferSize(65535);
    socket.setReceiveBufferSize(65535);

    final OutputStream outputStream = socket.getOutputStream();
    final InputStream inputStream = socket.getInputStream();

    handshake(username, password, outputStream, inputStream);

    return socket;
  }

  private void handshake(String username, String password, OutputStream outputStream,
      InputStream inputStream) throws IOException {
    sendVersionMessage(outputStream);
    sendHandshake(username, password, outputStream);
    readVersionResponse(inputStream);
    readHandshakeResponse(username, inputStream);
  }

  private void sendVersionMessage(OutputStream outputStream) throws IOException {
    ProtocolVersion.NewConnectionClientVersion.newBuilder()
        .setMajorVersion(ProtocolVersion.MajorVersions.CURRENT_MAJOR_VERSION_VALUE)
        .setMinorVersion(ProtocolVersion.MinorVersions.CURRENT_MINOR_VERSION_VALUE).build()
        .writeDelimitedTo(outputStream);
  }

  private void readVersionResponse(InputStream inputStream) throws IOException {
    if (!ProtocolVersion.VersionAcknowledgement.parseDelimitedFrom(inputStream)
        .getVersionAccepted()) {
      throw new IOException("Failed protocol version verification.");
    }
  }

  /**
   * Queries locators for a Geode server that has Protobuf enabled.
   *
   * @return The server chosen by the Locator service for this client
   */
  private InetSocketAddress findAServer(final Set<InetSocketAddress> locators, String username,
      String password, String keyStorePath, String trustStorePath, String protocols, String ciphers)
      throws GeneralSecurityException, IOException {
    IOException lastException = null;

    for (InetSocketAddress locator : locators) {
      Socket locatorSocket = null;
      try {
        locatorSocket = createSocket(locator.getAddress(), locator.getPort(), keyStorePath,
            trustStorePath, protocols, ciphers);

        final OutputStream outputStream = locatorSocket.getOutputStream();
        final InputStream inputStream = locatorSocket.getInputStream();

        handshake(username, password, outputStream, inputStream);

        ClientProtocol.Message.newBuilder()
            .setGetServerRequest(LocatorAPI.GetServerRequest.newBuilder()).build()
            .writeDelimitedTo(outputStream);

        ClientProtocol.Message response = ClientProtocol.Message.parseDelimitedFrom(inputStream);

        if (response == null) {
          throw new IOException("Server terminated connection");
        }

        ClientProtocol.ErrorResponse errorResponse = response.getErrorResponse();

        if (errorResponse != null && errorResponse.hasError()) {
          throw new IOException(
              "Error finding server: error code= " + errorResponse.getError().getErrorCode()
                  + "; error message=" + errorResponse.getError().getMessage());
        }

        LocatorAPI.GetServerResponse getServerResponse = response.getGetServerResponse();

        BasicTypes.Server server = getServerResponse.getServer();
        return new InetSocketAddress(server.getHostname(), server.getPort());
      } catch (IOException e) {
        lastException = e;
      } finally {
        if (locatorSocket != null) {
          locatorSocket.setSoLinger(true, 0);
          locatorSocket.close();
        }
      }
    }

    if (lastException != null) {
      throw lastException;
    } else {
      throw new IllegalStateException("No locators");
    }
  }

  private void authenticate(String username, String password, OutputStream outputStream,
      InputStream inputStream) throws IOException {
    sendHandshake(username, password, outputStream);

    readHandshakeResponse(username, inputStream);
  }

  private void readHandshakeResponse(String username, InputStream inputStream) throws IOException {
    final ClientProtocol.Message authenticationResponseMessage =
        ClientProtocol.Message.parseDelimitedFrom(inputStream);
    final ClientProtocol.ErrorResponse errorResponse =
        authenticationResponseMessage.getErrorResponse();
    if (!Objects.isNull(errorResponse) && errorResponse.hasError()) {
      throw new IOException("Failed authentication for " + username + ": error code="
          + errorResponse.getError().getErrorCode() + "; error message="
          + errorResponse.getError().getMessage());
    }
    final ConnectionAPI.HandshakeResponse authenticationResponse =
        authenticationResponseMessage.getHandshakeResponse();
    if (username != null && !Objects.isNull(authenticationResponse)
        && !authenticationResponse.getAuthenticated()) {
      throw new IOException("Failed authentication for " + username);
    }
  }

  private void sendHandshake(String username, String password, OutputStream outputStream)
      throws IOException {
    final ConnectionAPI.HandshakeRequest.Builder builder =
        ConnectionAPI.HandshakeRequest.newBuilder();

    if (username != null) {
      builder.putCredentials("security-username", username);
      builder.putCredentials("security-password", password);
    }

    builder.setValueFormat(serializer.getID());

    final ClientProtocol.Message authenticationRequest =
        ClientProtocol.Message.newBuilder().setHandshakeRequest(builder).build();
    authenticationRequest.writeDelimitedTo(outputStream);
  }

  private Socket createSocket(InetAddress host, int port, String keyStorePath,
      String trustStorePath, String protocols, String ciphers)
      throws GeneralSecurityException, IOException {
    return new SocketFactory().setHost(host).setPort(port).setTimeout(5000)
        .setKeyStorePath(keyStorePath).setTrustStorePath(trustStorePath).setProtocols(protocols)
        .setCiphers(ciphers).connect();
  }
}
