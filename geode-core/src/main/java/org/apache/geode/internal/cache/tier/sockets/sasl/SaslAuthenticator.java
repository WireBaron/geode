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

package org.apache.geode.internal.cache.tier.sockets.sasl;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import javax.security.sasl.SaslServer;

import org.apache.geode.security.SecurityManager;
import org.apache.logging.log4j.Logger;

import org.apache.geode.internal.logging.LogService;

/**
 * SaslAuthenticator performs simple authentication using SASL
 */
public class SaslAuthenticator implements Authenticator {
  protected static final Logger logger = LogService.getLogger();
  private SecurityManager securityManager;

  public SaslAuthenticator(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public boolean authenticateClient() {
    return false;
  }

  @Override
  public Optional<String> handleHandshakeRequest(Collection<String> mechanisms) {
    if (mechanisms.contains("PLAIN")) {
      return Optional.of("PLAIN");
    } else {
      return Optional.empty();
    }
  }

  @Override
  public AuthenticationProgress handleAuthenticationRequest(Collection<Object> parameters) {
    return null;
  }
}
