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

package org.apache.geode.internal.cache.tier.sockets;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.wan.GatewayTransportFilter;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.cache.InternalCache;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class AcceptorConfiguration {
  private int port;
  private final String bindHostName;
  private final boolean notifyBySubscription;
  private final int socketBufferSize;
  private final int maximumTimeBetweenPings;
  private final InternalCache internalCache;
  private final int maxConnections;
  private final int maxThreads;
  private final int maximumMessageCount;
  private final int messageTimeToLive;
  private final ConnectionListener connectionListener;
  private final List overflowAttributesList;
  private final boolean isGatewayReceiver;
  private final List<GatewayTransportFilter> transportFilter;
  private final boolean tcpNoDelay;

  /**
   * @param port The port on which this acceptor listens for connections. If <code>0</code>, a
   *        random port will be chosen.
   * @param bindHostName The ip address or host name this acceptor listens on for connections. If
   *        <code>null</code> or "" then all local addresses are used
   * @param socketBufferSize The buffer size for server-side sockets
   * @param maximumTimeBetweenPings The maximum time between client pings. This value is used by the
   *        <code>ClientHealthMonitor</code> to monitor the health of this server's clients.
   * @param internalCache The GemFire cache whose contents is served to clients
   * @param maxConnections the maximum number of connections allowed in the server pool
   * @param maxThreads the maximum number of threads allowed in the server pool
   */
  public AcceptorConfiguration(int port, String bindHostName, boolean notifyBySubscription,
      int socketBufferSize, int maximumTimeBetweenPings, InternalCache internalCache,
      int maxConnections, int maxThreads, int maximumMessageCount, int messageTimeToLive,
      ConnectionListener listener, List overflowAttributesList, boolean isGatewayReceiver,
      List<GatewayTransportFilter> transportFilter, boolean tcpNoDelay) {
    this.port = port;
    this.bindHostName = calcBindHostName(internalCache, bindHostName);
    this.notifyBySubscription = notifyBySubscription;
    this.socketBufferSize = socketBufferSize;
    this.maximumTimeBetweenPings = maximumTimeBetweenPings;
    this.internalCache = internalCache;
    this.maxConnections = maxConnections;
    this.maxThreads = maxThreads;
    this.maximumMessageCount = maximumMessageCount;
    this.messageTimeToLive = messageTimeToLive;
    this.connectionListener = listener == null ? new ConnectionListenerAdapter() : listener;;
    this.overflowAttributesList = overflowAttributesList;
    this.isGatewayReceiver = isGatewayReceiver;
    this.transportFilter = transportFilter;
    this.tcpNoDelay = tcpNoDelay;
  }

  public int getPort() {
    return port;
  }

  public String getBindHostName() {
    return bindHostName;
  }

  public boolean isNotifyBySubscription() {
    return notifyBySubscription;
  }

  public int getSocketBufferSize() {
    return socketBufferSize;
  }

  public int getMaximumTimeBetweenPings() {
    return maximumTimeBetweenPings;
  }

  public InternalCache getInternalCache() {
    return internalCache;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public int getMaximumMessageCount() {
    return maximumMessageCount;
  }

  public int getMessageTimeToLive() {
    return messageTimeToLive;
  }

  public ConnectionListener getConnectionListener() {
    return connectionListener;
  }

  public List getOverflowAttributesList() {
    return overflowAttributesList;
  }

  public boolean isGatewayReceiver() {
    return isGatewayReceiver;
  }

  public List<GatewayTransportFilter> getTransportFilter() {
    return transportFilter;
  }

  public boolean isTcpNoDelay() {
    return tcpNoDelay;
  }

  public void setPort(int port) {
    this.port = port;
  }

  /**
   * @param bindName the ip address or host name that this acceptor should bind to. If null or ""
   *        then calculate it.
   * @return the ip address or host name this acceptor will listen on. An "" if all local addresses
   *         will be listened to.
   * @since GemFire 5.7
   */
  public String calcBindHostName(Cache cache, String bindName) {
    if (bindName != null && !bindName.equals("")) {
      return bindName;
    }

    InternalDistributedSystem system = (InternalDistributedSystem) cache.getDistributedSystem();
    DistributionConfig config = system.getConfig();
    String hostName = null;

    // Get the server-bind-address. If it is not null, use it.
    // If it is null, get the bind-address. If it is not null, use it.
    // Otherwise set default.
    String serverBindAddress = config.getServerBindAddress();
    if (serverBindAddress != null && serverBindAddress.length() > 0) {
      hostName = serverBindAddress;
    } else {
      String bindAddress = config.getBindAddress();
      if (bindAddress != null && bindAddress.length() > 0) {
        hostName = bindAddress;
      }
    }
    return hostName;
  }

  public InetAddress getBindAddress() throws IOException {
    if (getBindHostName() == null || "".equals(getBindHostName())) {
      return null; // pick default local address
    } else {
      return InetAddress.getByName(getBindHostName());
    }
  }
}
