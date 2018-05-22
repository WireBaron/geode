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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.test.junit.categories.ClientServerTest;
import org.apache.geode.test.junit.categories.IntegrationTest;

@Category({IntegrationTest.class, ClientServerTest.class})
public class MultithreadedConnectionTest extends IntegrationTestBase {
  static final String COMMON_KEY = "commonKey";
  static final String WORKER_VALUE_PREFIX = "workerUniqueValue";
  static final int CONCURRENT_THREADS = 100;
  static final int PUTS_PER_THREAD = 1000;

  Random random = new Random();

  public class Worker extends Thread {
    Region region;
    int iterations;
    // Half of operations will set the common key to our thread unique value
    String valueForCommonKey;

    // The other half will set our unique key to new value (and check the value before and after)
    String workerUniqueKey;
    int workerUniqueValueSuffix;
    boolean uniqueIsSet;

    public Worker(Region<String, String> testRegion, String uniqueKey, String uniqueValue,
        int numOps) {
      region = testRegion;
      valueForCommonKey = uniqueValue;
      iterations = numOps;

      workerUniqueKey = uniqueKey;
      workerUniqueValueSuffix = 0;
      uniqueIsSet = false;
    }

    @Override
    public void run() {
      for (int i = 0; i < iterations; ++i) {
        try {
          if (random.nextBoolean()) {
            performCommonPutOperation();
          } else {
            performUniquePutOperation();
          }
        } catch (IOException e) {
          throw new RuntimeException("Put failed, i = " + i, e);
        }
      }
    }

    private void performCommonPutOperation() throws IOException {
      region.put(COMMON_KEY, valueForCommonKey);
    }

    private void performUniquePutOperation() throws IOException {
      if (uniqueIsSet) {
        assertEquals(WORKER_VALUE_PREFIX + workerUniqueValueSuffix, region.get(workerUniqueKey));
      } else {
        uniqueIsSet = true;
      }
      ++workerUniqueValueSuffix;
      region.put(workerUniqueKey, WORKER_VALUE_PREFIX + workerUniqueValueSuffix);
      assertEquals(WORKER_VALUE_PREFIX + workerUniqueValueSuffix, region.get(workerUniqueKey));
    }
  }

  @Test
  public void multipleThreadsRunConcurrently() throws Exception {
    final Region<String, String> testRegion = driver.getRegion("region");

    List<Worker> workerList = new ArrayList<>(CONCURRENT_THREADS);
    for (int i = 0; i < CONCURRENT_THREADS; ++i) {
      workerList.add(
          new Worker(testRegion, "WORKER_UNIQUE_" + i, "LAST_TOUCHED_BY_" + i, PUTS_PER_THREAD));
    }

    for (Worker worker : workerList) {
      worker.start();
    }

    int numValues = 1;
    for (Worker worker : workerList) {
      worker.join();
      if (worker.uniqueIsSet) {
        ++numValues;
      }
    }

    assertEquals(numValues, testRegion.size());
    String result = testRegion.get(COMMON_KEY);
    assertTrue(result.startsWith("LAST_TOUCHED_BY_"));
  }
}
