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
package org.apache.geode.internal.cache.backup;

import java.util.Set;

import org.apache.geode.distributed.internal.DM;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.admin.remote.AdminResponse;
import org.apache.geode.internal.admin.remote.CliLegacyMessage;

/**
 * A request to from an admin VM to all non admin members to start a backup. In the prepare phase of
 * the backup, the members will suspend bucket destroys to make sure buckets aren't missed during
 * the backup.
 */
public class FlushToDiskRequest extends CliLegacyMessage {

  private final FlushToDiskFactory flushToDiskFactory;

  public FlushToDiskRequest() {
    super();
    this.flushToDiskFactory = new FlushToDiskFactory();
  }

  FlushToDiskRequest(InternalDistributedMember sender, Set<InternalDistributedMember> recipients,
      int processorId, FlushToDiskFactory flushToDiskFactory) {
    this.setSender(sender);
    setRecipients(recipients);
    this.msgId = processorId;
    this.flushToDiskFactory = flushToDiskFactory;
  }

  @Override
  public int getDSFID() {
    return FLUSH_TO_DISK_REQUEST;
  }

  @Override
  protected AdminResponse createResponse(DM dm) {
    flushToDiskFactory.createFlushToDisk(dm.getCache()).run();
    return flushToDiskFactory.createResponse(getSender());
  }

}
