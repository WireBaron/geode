package org.apache.geode.internal.cache.tier.sockets.sasl;

import org.apache.geode.cache.Cache;

public class ExecutionContext {
  private final Cache cache;
  private final Authenticator authenticator;

  public ExecutionContext(Cache cache,
                          Authenticator authenticator) {
    this.cache = cache;
    this.authenticator = authenticator;
  }

  public Cache getCache() {
    return cache;
  }

  public Authenticator getAuthenticator() {
    return authenticator;
  }
}
