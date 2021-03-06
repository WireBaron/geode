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
package org.apache.geode.internal.cache.eviction;

import org.apache.geode.internal.cache.versions.RegionVersionVector;

public interface EvictionList {

  void setBucketRegion(Object region);

  void closeStats();

  /**
   * Adds a new eviction node for the entry between the current tail and head of the list.
   */
  void appendEntry(EvictionNode evictionNode);

  /**
   * Returns the Entry that is considered least recently used. The entry will no longer be in the
   * pipe (unless it is the last empty marker).
   */
  EvictableEntry getEvictableEntry();

  /**
   * remove an entry from the list
   */
  void destroyEntry(EvictionNode evictionNode);

  /**
   * Get the modifier for lru based statistics.
   *
   * @return The EvictionStatistics for this Clock hand's region.
   */
  EvictionStatistics getStatistics();

  /**
   * called when an LRU map is cleared... resets stats and releases prev and next.
   */
  void clear(RegionVersionVector regionVersionVector);

  /**
   * Returns the number of EvictionNodes in the EvictionList.
   */
  int size();

  void incrementRecentlyUsed();
}
