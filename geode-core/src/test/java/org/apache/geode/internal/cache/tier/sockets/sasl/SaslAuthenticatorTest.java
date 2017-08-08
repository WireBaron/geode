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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.apache.geode.security.SecurityManager;
import org.eclipse.jetty.util.ArrayUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class SaslAuthenticatorTest {

  private SecurityManager securityManager;
  private SaslAuthenticator saslAuthenticator;

  @Before
  public void setUp() throws Exception {
    securityManager = mock(SecurityManager.class);
    saslAuthenticator = new SaslAuthenticator(securityManager);
  }

  @Test
  public void respondsToHandshakeWithSelectedMechanism() {
    Collection<String> requestedMechanisms = Arrays.asList(new String[] {"MD5", "WD40", "PLAIN", "foo"});
    Optional<String> handshakeResult = saslAuthenticator.handleHandshakeRequest(requestedMechanisms);
    assertTrue(handshakeResult.isPresent());
    assertEquals("PLAIN", handshakeResult.get());
  }

  @Test
  public void emptyResultToHandshakeWithNoValidMechanism() {
    Collection<String> requestedMechanisms = Arrays.asList(new String[] {"MD5", "WD40", "foo"});
    Optional<String> handshakeResult = saslAuthenticator.handleHandshakeRequest(requestedMechanisms);
    assertFalse(handshakeResult.isPresent());
  }
}
