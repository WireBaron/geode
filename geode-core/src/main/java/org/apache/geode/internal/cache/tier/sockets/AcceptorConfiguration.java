package org.apache.geode.internal.cache.tier.sockets;

import org.apache.geode.cache.wan.GatewayTransportFilter;
import org.apache.geode.internal.cache.InternalCache;

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
  private final ConnectionListener listener;
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
*
   */
  public AcceptorConfiguration(int port, String bindHostName, boolean notifyBySubscription, int socketBufferSize, int maximumTimeBetweenPings, InternalCache internalCache, int maxConnections, int maxThreads, int maximumMessageCount, int messageTimeToLive, ConnectionListener listener, List overflowAttributesList, boolean isGatewayReceiver, List<GatewayTransportFilter> transportFilter, boolean tcpNoDelay) {
    this.port = port;
    this.bindHostName = bindHostName;
    this.notifyBySubscription = notifyBySubscription;
    this.socketBufferSize = socketBufferSize;
    this.maximumTimeBetweenPings = maximumTimeBetweenPings;
    this.internalCache = internalCache;
    this.maxConnections = maxConnections;
    this.maxThreads = maxThreads;
    this.maximumMessageCount = maximumMessageCount;
    this.messageTimeToLive = messageTimeToLive;
    this.listener = listener;
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

  public ConnectionListener getListener() {
    return listener;
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
}
