package org.apache.geode.internal.cache.tier.sockets.sasl;

import java.util.Collection;
import java.util.Optional;

public interface Authenticator {
  public enum AuthenticationProgress {
    AUTHENTICATION_IN_PROGRESS,
    AUTHENTICATION_SUCCEEDED,
    AUTHENTICATION_FAILED
  }

  public Optional<String> handleHandshakeRequest(Collection<String> mechanisms);

  AuthenticationProgress handleAuthenticationRequest(Collection<Object> parameters);

}
