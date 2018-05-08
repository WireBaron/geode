/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geode.cache.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.config.JAXBService;
import org.apache.geode.test.junit.categories.UnitTest;


@Category(UnitTest.class)
public class CacheConfigTest {

  private CacheConfig cacheConfig;
  private JAXBService service;
  private RegionConfig regionConfig;
  private String regionXml;
  private DeclarableType declarableWithString;
  private String declarableWithStringXml;
  private ClassNameType classNameType;
  private String classNameTypeXml;
  private DeclarableType declarableWithParam;
  private String declarableWithParamXml;
  private String stringTypeXml;
  private String cacheXml;

  @Before
  public void setUp() throws Exception {
    cacheConfig = new CacheConfig("1.0");
    cacheXml =
        "<cache version=\"1.0\" xsi:schemaLocation=\"http://geode.apache.org/schema/cache http://geode.apache.org/schema/cache/cache-1.0.xsd\" xmlns=\"http://geode.apache.org/schema/cache\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">";
    service = new JAXBService(CacheConfig.class);
    service.validateWithLocalCacheXSD();
    regionConfig = new RegionConfig();
    regionConfig.setName("regionA");
    regionConfig.setRefid("REPLICATE");
    regionXml = "<region name=\"regionA\" refid=\"REPLICATE\">";

    classNameType = new ClassNameType("my.className");
    classNameTypeXml = "<class-name>my.className</class-name>";
    declarableWithString = new DeclarableType("my.className", "{'key':'value'}");
    declarableWithStringXml =
        classNameTypeXml + "<parameter name=\"key\"><string>value</string></parameter>";

    declarableWithParam = new DeclarableType("my.className");
    ParameterType param = new ParameterType("key");
    param.setDeclarable(declarableWithString);
    declarableWithParam.getParameter().add(param);
    declarableWithParamXml = classNameTypeXml + "<parameter name=\"key\"><declarable>"
        + declarableWithStringXml + "</declarable></parameter>";

    stringTypeXml = "<string>string value</string>";
  }

  @After
  public void tearDown() throws Exception {
    // make sure the marshalled xml passed validation
    System.out.println(service.marshall(cacheConfig));
  }

  @Test
  public void indexType() {
    String xml = cacheXml + regionXml
        + "<index name=\"indexName\" expression=\"expression\" key-index=\"true\"/>"
        + "</region></cache>";

    cacheConfig = service.unMarshall(xml);
    RegionConfig.Index index = cacheConfig.getRegion().get(0).getIndex().get(0);
    assertThat(index.isKeyIndex()).isTrue();
    assertThat(index.getName()).isEqualTo("indexName");
    assertThat(index.getExpression()).isEqualTo("expression");
    assertThat(index.getType()).isEqualTo("range");
  }


  @Test
  public void regionEntry() {
    String xml = cacheXml + regionXml + "<entry>" + "<key><string>key1</string></key>"
        + "<value><declarable>" + declarableWithStringXml + "</declarable></value>" + "</entry>"
        + "<entry>" + "<key><string>key2</string></key>" + "<value><declarable>"
        + declarableWithParamXml + "</declarable></value>" + "</entry>" + "</region></cache>";

    cacheConfig = service.unMarshall(xml);
    RegionConfig.Entry entry = cacheConfig.getRegion().get(0).getEntry().get(0);
    assertThat(entry.getKey().toString()).isEqualTo("key1");
    assertThat(entry.getValue().getDeclarable()).isEqualTo(declarableWithString);

    entry = cacheConfig.getRegion().get(0).getEntry().get(1);
    assertThat(entry.getKey().toString()).isEqualTo("key2");
    assertThat(entry.getValue().getDeclarable()).isEqualTo(declarableWithParam);
  }

  @Test
  public void cacheTransactionManager() {
    String xml = cacheXml + "<cache-transaction-manager>" + "<transaction-listener>"
        + declarableWithStringXml + "</transaction-listener>" + "<transaction-writer>"
        + declarableWithStringXml + "</transaction-writer>"
        + "</cache-transaction-manager></cache>";

    cacheConfig = service.unMarshall(xml);
    assertThat(cacheConfig.getCacheTransactionManager().getTransactionWriter())
        .isEqualTo(declarableWithString);
    assertThat(cacheConfig.getCacheTransactionManager().getTransactionListener().get(0))
        .isEqualTo(declarableWithString);
  }

  @Test
  public void declarables() {
    String xml = cacheXml + "<region-attributes>" + "<cache-loader>" + declarableWithStringXml
        + "</cache-loader>" + "<cache-listener>" + declarableWithStringXml + "</cache-listener>"
        + "<cache-writer>" + declarableWithStringXml + "</cache-writer>" + "<compressor>"
        + classNameTypeXml + "</compressor>"
        + "<region-time-to-live><expiration-attributes timeout=\"0\"><custom-expiry>"
        + declarableWithStringXml + "</custom-expiry></expiration-attributes></region-time-to-live>"
        + "</region-attributes>" + "<function-service><function>" + declarableWithStringXml
        + "</function></function-service>" + "<initializer>" + declarableWithStringXml
        + "</initializer>" + "<pdx><pdx-serializer>" + declarableWithStringXml
        + "</pdx-serializer></pdx>" + "<cache-server><custom-load-probe>" + declarableWithStringXml
        + "</custom-load-probe></cache-server>" + "<gateway-conflict-resolver>"
        + declarableWithStringXml + "</gateway-conflict-resolver>"
        + "<gateway-receiver><gateway-transport-filter>" + declarableWithStringXml
        + "</gateway-transport-filter></gateway-receiver>" + "<async-event-queue id=\"queue\">"
        + "<gateway-event-substitution-filter>" + declarableWithStringXml
        + "</gateway-event-substitution-filter>" + "<gateway-event-filter>"
        + declarableWithStringXml + "</gateway-event-filter>" + "<async-event-listener>"
        + declarableWithStringXml + "</async-event-listener>" + "</async-event-queue>"
        + "<gateway-hub id=\"hub\"><gateway id=\"gateway\"><gateway-listener>"
        + declarableWithStringXml + "</gateway-listener></gateway></gateway-hub>" + "</cache>";

    cacheConfig = service.unMarshall(xml);

    assertThat(cacheConfig.getInitializer()).isEqualTo(declarableWithString);
    assertThat(cacheConfig.getFunctionService().getFunction().get(0))
        .isEqualTo(declarableWithString);
    assertThat(cacheConfig.getPdx().getPdxSerializer()).isEqualTo(declarableWithString);
    assertThat(cacheConfig.getCacheServer().get(0).getCustomLoadProbe())
        .isEqualTo(declarableWithString);
    assertThat(cacheConfig.getGatewayConflictResolver()).isEqualTo(declarableWithString);
    assertThat(cacheConfig.getGatewayReceiver().getGatewayTransportFilter().get(0))
        .isEqualTo(declarableWithString);
    assertThat(cacheConfig.getGatewayHub().get(0).getGateway().get(0).getGatewayListener().get(0))
        .isEqualTo(declarableWithString);

    CacheConfig.AsyncEventQueue asyncEventQueue = cacheConfig.getAsyncEventQueue().get(0);
    assertThat(asyncEventQueue.getAsyncEventListener()).isEqualTo(declarableWithString);
    assertThat(asyncEventQueue.getGatewayEventFilter().get(0)).isEqualTo(declarableWithString);
    assertThat(asyncEventQueue.getGatewayEventSubstitutionFilter()).isEqualTo(declarableWithString);

    RegionAttributesType regionAttributes = cacheConfig.getRegionAttributes().get(0);
    assertThat(regionAttributes.getCacheListener().get(0)).isEqualTo(declarableWithString);
    assertThat(regionAttributes.getCacheLoader()).isEqualTo(declarableWithString);
    assertThat(regionAttributes.getCacheWriter()).isEqualTo(declarableWithString);
    assertThat(regionAttributes.getRegionTimeToLive().getExpirationAttributes().getCustomExpiry())
        .isEqualTo(declarableWithString);
  }
}