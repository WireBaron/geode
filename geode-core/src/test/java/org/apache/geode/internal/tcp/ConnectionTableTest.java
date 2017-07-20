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
package org.apache.geode.internal.tcp;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.client.internal.SSLNoClientAuthDUnitTest;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.Locator;
import org.apache.geode.internal.security.SecurableCommunicationChannel;
import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.LogWriterUtils;
import org.apache.geode.test.dunit.RMIException;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.internal.JUnit4DistributedTestCase;
import org.apache.geode.test.junit.categories.IntegrationTest;
import org.apache.geode.util.test.TestUtil;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.geode.distributed.ConfigurationProperties.DISABLE_AUTO_RECONNECT;
import static org.apache.geode.distributed.ConfigurationProperties.DURABLE_CLIENT_ID;
import static org.apache.geode.distributed.ConfigurationProperties.ENABLE_CLUSTER_CONFIGURATION;
import static org.apache.geode.distributed.ConfigurationProperties.ENABLE_NETWORK_PARTITION_DETECTION;
import static org.apache.geode.distributed.ConfigurationProperties.LOCATORS;
import static org.apache.geode.distributed.ConfigurationProperties.LOG_LEVEL;
import static org.apache.geode.distributed.ConfigurationProperties.MCAST_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.MEMBER_TIMEOUT;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_CIPHERS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_ENABLED_COMPONENTS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_PROTOCOLS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_REQUIRE_AUTHENTICATION;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(IntegrationTest.class)
public class ConnectionTableTest extends JUnit4DistributedTestCase {
  private static Host host;
  private static Cache cache;

  private static final String DEFAULT_STORE = "default.keystore";

  private static void createCache(int locatorPort) throws Exception {

    Properties properties = new Properties();
    System.setProperty("p2p.oldIO", "true");
    properties.setProperty("conserve-sockets", "false");

    //Boolean.setBoolean("p2p.oldIO");
    properties.setProperty(MCAST_PORT, "0");
    properties.setProperty(LOCATORS, "localhost[" + locatorPort + "]");


    String keyStore = TestUtil.getResourcePath(SSLNoClientAuthDUnitTest.class, DEFAULT_STORE);
    String trustStore = TestUtil.getResourcePath(SSLNoClientAuthDUnitTest.class, DEFAULT_STORE);
    properties.put(MCAST_PORT, "0");
    properties.put(ENABLE_NETWORK_PARTITION_DETECTION, "false");
    properties.put(DISABLE_AUTO_RECONNECT, "true");
    properties.put(MEMBER_TIMEOUT, "2000");
    properties.put(LOG_LEVEL, LogWriterUtils.getDUnitLogLevel());
    properties.put(ENABLE_CLUSTER_CONFIGURATION, "false");
    properties.put(SSL_CIPHERS, "any");
    properties.put(SSL_PROTOCOLS, "any");
    properties.put(SSL_KEYSTORE, keyStore);
    properties.put(SSL_KEYSTORE_PASSWORD, "password");
    properties.put(SSL_KEYSTORE_TYPE, "JKS");
    properties.put(SSL_TRUSTSTORE, trustStore);
    properties.put(SSL_TRUSTSTORE_PASSWORD, "password");
    properties.put(SSL_REQUIRE_AUTHENTICATION, "false");
    properties.put(SSL_ENABLED_COMPONENTS, SecurableCommunicationChannel.ALL.getConstant());
//    properties.put(CLUSTER_SSL_ENABLED, true );
//    properties.put(SSL_ENABLED_COMPONENTS, "all");
//    properties.put(SERVER_SSL_ENABLED, String.valueOf(true));
//    properties.put(SERVER_SSL_PROTOCOLS, "any");
//    properties.put(SERVER_SSL_CIPHERS, "any");
//    properties.put(SERVER_SSL_REQUIRE_AUTHENTICATION, String.valueOf(true));
//
//    properties.put(SERVER_SSL_KEYSTORE_TYPE, "jks");
//    properties.put(SERVER_SSL_KEYSTORE, keyStore);
//    properties.put(SERVER_SSL_KEYSTORE_PASSWORD, "password");
//    properties.put(SERVER_SSL_TRUSTSTORE, trustStore);
//    properties.put(SERVER_SSL_TRUSTSTORE_PASSWORD, "password");

    cache = new CacheFactory(properties).create();
    CacheServer cacheServer = cache.addCacheServer();
    cacheServer.setPort(0);
    cacheServer.start();
    if (cache == null) {
      throw new Exception("CacheFactory.create() returned null ");
    }
  }

  private static void doPuts() {
    Region<Object, Object> region1 = cache.getRegion("region1");

    int n = 100;
    ExecutorService executor = Executors.newFixedThreadPool(n);

    for (int i = 0; i < n; i++) {

      executor.execute(new Runnable() {
        @Override
        public void run() {
          for(int i =0 ; i < 10000 ; i++)
            region1.put(i, i);
        }
      });
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
    System.out.println("Finished all threads");
  }

//  @Override
//  public Properties getDistributedSystemProperties() {
//  }

  private void createRegion(String name, RegionShortcut regionType) {
    cache.createRegionFactory(regionType).create(name);
  }

  private void closeCache() {
    cache.close();
  }

  private static void clientStuff(String hostname, int port) throws InterruptedException {
    Properties properties = new Properties();
    String keyStore = TestUtil.getResourcePath(SSLNoClientAuthDUnitTest.class, DEFAULT_STORE);
    String trustStore = TestUtil.getResourcePath(SSLNoClientAuthDUnitTest.class, DEFAULT_STORE);
//    properties.put(SERVER_SSL_ENABLED, String.valueOf(true));
//    properties.put(SERVER_SSL_PROTOCOLS, "any");
//    properties.put(SERVER_SSL_CIPHERS, "any");
//    properties.put(SERVER_SSL_REQUIRE_AUTHENTICATION, String.valueOf(true));
//
//    properties.put(SERVER_SSL_KEYSTORE_TYPE, "jks");
//    properties.put(SERVER_SSL_KEYSTORE, keyStore);
//    properties.put(SERVER_SSL_KEYSTORE_PASSWORD, "password");
//    properties.put(SERVER_SSL_TRUSTSTORE, trustStore);
//    properties.put(SERVER_SSL_TRUSTSTORE_PASSWORD, "password");
    properties.put(MCAST_PORT, "0");
    properties.put(ENABLE_NETWORK_PARTITION_DETECTION, "false");
    properties.put(DISABLE_AUTO_RECONNECT, "true");
    properties.put(MEMBER_TIMEOUT, "2000");
    properties.put(LOG_LEVEL, LogWriterUtils.getDUnitLogLevel());
    properties.put(ENABLE_CLUSTER_CONFIGURATION, "false");
    properties.put(SSL_CIPHERS, "any");
    properties.put(SSL_PROTOCOLS, "any");
//    properties.put(SSL_KEYSTORE, keyStore);
//    properties.put(SSL_KEYSTORE_PASSWORD, "password");
//    properties.put(SSL_KEYSTORE_TYPE, "JKS");
    properties.put(SSL_TRUSTSTORE, trustStore);
    properties.put(SSL_TRUSTSTORE_PASSWORD, "password");
    properties.put(SSL_REQUIRE_AUTHENTICATION, "true");
    properties.put(SSL_ENABLED_COMPONENTS, SecurableCommunicationChannel.ALL.getConstant());
//    p.put("log-level", "fine");
    ClientCacheFactory clientCacheFactory = new ClientCacheFactory(properties);
    clientCacheFactory.addPoolLocator(hostname, port);
//    clientCacheFactory.addPoolServer(DistributedTestUtils.)
    ClientCache clientCache = clientCacheFactory.create();
    System.out.println(" = " + clientCache.getCurrentServers());
    Region<Object, Object> region1 = clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY).create("region1");

    int n = 100;
    ExecutorService executor = Executors.newFixedThreadPool(n);

    for (int i = 0; i < n; i++) {

      executor.execute(new Runnable() {
        @Override
        public void run() {
          for(int i =0 ; i < 10000 ; i++)
          region1.put(i, i);
        }
      });
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
    }
    System.out.println("Finished all threads");



    Thread.sleep(1000);
    clientCache.close();
  }

  private int startSSLLocator() throws IOException {

    String keyStore = TestUtil.getResourcePath(SSLNoClientAuthDUnitTest.class, DEFAULT_STORE);
    String trustStore = TestUtil.getResourcePath(SSLNoClientAuthDUnitTest.class, DEFAULT_STORE);
    final Properties properties = new Properties();
    properties.put(MCAST_PORT, "0");
    properties.put(ENABLE_NETWORK_PARTITION_DETECTION, "false");
    properties.put(DISABLE_AUTO_RECONNECT, "true");
    properties.put(MEMBER_TIMEOUT, "2000");
    properties.put(LOG_LEVEL, LogWriterUtils.getDUnitLogLevel());
    properties.put(ENABLE_CLUSTER_CONFIGURATION, "false");
    properties.put(SSL_CIPHERS, "any");
    properties.put(SSL_PROTOCOLS, "any");
    properties.put(SSL_KEYSTORE, keyStore);
    properties.put(SSL_KEYSTORE_PASSWORD, "password");
    properties.put(SSL_KEYSTORE_TYPE, "JKS");
    properties.put(SSL_TRUSTSTORE, trustStore);
    properties.put(SSL_TRUSTSTORE_PASSWORD, "password");
    properties.put(SSL_REQUIRE_AUTHENTICATION, "true");
    properties.put(SSL_ENABLED_COMPONENTS, SecurableCommunicationChannel.ALL.getConstant());

    Locator locator = Locator.startLocatorAndDS(0, new File(""), properties);
    return locator.getPort();
  }

  private ClientCache setupGemFireClientCache(String locatorHostname, int locatorPort) {


    ClientCache clientCache =
        new ClientCacheFactory().set(DURABLE_CLIENT_ID, "TestDurableClientId").create();

    PoolFactory poolFactory = PoolManager.createFactory();

    poolFactory.setMaxConnections(10);
    poolFactory.setMinConnections(1);
    poolFactory.setReadTimeout(5000);
    poolFactory.setSubscriptionEnabled(true);
    poolFactory.addLocator(locatorHostname, locatorPort);

    Pool pool = poolFactory.create("serverConnectionPool");

    assertNotNull("The 'serverConnectionPool' was not properly configured and initialized!", pool);

//    ClientRegionFactory<String, String> regionFactory =
//        clientCache.createClientRegionFactory(ClientRegionShortcut.CACHING_PROXY);
//
//    regionFactory.addCacheListener(new ClientServerRegisterInterestsDUnitTest.TestEntryCacheListener());
//    regionFactory.setPoolName(pool.getName());
//    regionFactory.setKeyConstraint(String.class);
//    regionFactory.setValueConstraint(String.class);
//
//    Region<String, String> exampleCachingProxy = regionFactory.create("Example");
//
//    assertNotNull("The 'Example' Client Region was not properly configured and initialized",
//        exampleCachingProxy);
//
//    clientCache.readyForEvents();
//
//    exampleCachingProxy.registerInterest("ALL_KEYS", InterestResultPolicy.DEFAULT, false, true);

    return clientCache;
  }

  @Test
  public void testRegionOnOtherServerDoesNotAppear() throws Exception {
//    host = Host.getHost(0);
//
//    VM remoteServer = host.getVM(1);
//    VM remoteServer2 = host.getVM(2);
//
//    System.out.println("this.getSystem() = " + this.getSystem());
//    System.out.println("getSystemStatic().getAllOtherMembers().toString() = " + getSystemStatic()
//        .getAllOtherMembers().toString());
//
//    remoteServer.invoke("start remote server", () -> createCache());
//    remoteServer.invoke(() -> createRegion("region1", RegionShortcut.PARTITION));
//
//    remoteServer2.invoke("start remote server", () -> createCache());
//    remoteServer2.invoke(() -> createRegion("region1", RegionShortcut.PARTITION));
//
//    remoteServer.invoke(() -> cache.getRegion("region1").put("key","value"));
//    remoteServer2.invoke(() -> cache.getRegion("region1").put("key","value"));
//
//    remoteServer.invoke("close remote cache", () -> closeCache());
//    Thread.sleep(100000);



    //remoteServer.invoke("close remote cache", () -> closeCache());


//    cache.getRegion("region1").put("otherKey", "otherValue");

//    assertTrue(value instanceof String);
//    assertEquals("value", value);
//
//    remoteServer.invoke(() -> createRegion("vm1Region", RegionShortcut.PARTITION));
//    remoteServer.invoke(() -> cache.getRegion("vm1Region").put("key","value"));
//    assertNull(vm2.invoke(() -> cache.getRegion("vm1Region")));
//
//    assertEquals(new Integer(2), remoteServer.invoke(() -> cache.rootRegions().size()));
//    assertEquals(new Integer(1), vm2.invoke(() -> cache.rootRegions().size()));
  }

  @Test
  public void


  otherTest() throws Exception {
    host = Host.getHost(0);

    VM remoteServer = host.getVM(1);
    VM remoteServer2 = host.getVM(2);
    VM client = host.getVM(3);
    VM locator = host.getVM(4);

    System.out.println("this.getSystem() = " + this.getSystem());
    System.out.println("getSystemStatic().getAllOtherMembers().toString() = " + getSystemStatic()
        .getAllOtherMembers().toString());

    int locatorPort = locator.invoke(this::startSSLLocator);

    remoteServer.invoke("start remote server", () -> createCache(locatorPort));
    remoteServer2.invoke("start remote server", () -> createCache(locatorPort));
    remoteServer.invoke(() -> createRegion("region1", RegionShortcut.REPLICATE));
    remoteServer2.invoke(() -> createRegion("region1", RegionShortcut.REPLICATE));



    Thread.sleep(2000);
//    remoteServer2.invoke(() -> doPuts());
    String hostname = host.getHostName();
    try {
      for (int i = 0; i < 1; ++i) {
        client.invoke(() -> clientStuff(hostname, locatorPort));
        //client.bounce();
      }
    } catch (RMIException | NullPointerException ex) {
      System.out.println("Got NPE = " + ex);
    }

//
//    remoteServer.invoke(() -> cache.getRegion("region1").put("key","value"));
//    remoteServer2.invoke(() -> cache.getRegion("region1").put("key","value"));
//
//    remoteServer.invoke("close remote cache", () -> closeCache());
    Thread.sleep(1000000);
  }
}