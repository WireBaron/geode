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
package org.apache.geode.protocol.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.protobuf.ByteString;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.protocol.protobuf.v1.Struct;
import org.apache.geode.internal.protocol.protobuf.v1.TypedStruct;
import org.apache.geode.internal.protocol.protobuf.v1.Value;
import org.apache.geode.pdx.PdxInstance;

public class ProtobufTypedStructSerializer implements ValueSerializer {
  private ProtobufStructSerializer structSerializer = new ProtobufStructSerializer();

  private HashMap<Integer, List<String>> serverTypeMap = new HashMap<>();
  int nextServerIndex = 1;

  public boolean hasType(int typeId) {
    return serverTypeMap.containsKey(typeId);
  }

  // Use to get a new type id
  public synchronized int registerNewType(List<String> fieldNames) {
    int index = nextServerIndex++;
    serverTypeMap.put(index, fieldNames);
    return index;
  }

  @Override
  public ByteString serialize(Object object) throws IOException {
    PdxInstance pdxInstance = (PdxInstance) object;

    TypedStruct.Builder typedStructBuilder = TypedStruct.newBuilder();
    for (String fieldName : pdxInstance.getFieldNames()) {
      Object value = pdxInstance.getField(fieldName);
      Value serialized = structSerializer.serializeValue(value);
      typedStructBuilder.addFields(serialized);
    }

    return typedStructBuilder.build().toByteString();
  }

  @Override
  public Object deserialize(ByteString bytes) throws IOException, ClassNotFoundException {
    TypedStruct struct = TypedStruct.parseFrom(bytes);
    return structSerializer.deserialize(typedStructToStruct(struct));
  }

  @Override
  public void init(Cache cache) {
    structSerializer.init(cache);
  }

  private Struct typedStructToStruct(TypedStruct typedStruct)
      throws IOException, ClassNotFoundException {
    if (!serverTypeMap.containsKey(typedStruct.getTypeId())) {
      throw new ClassNotFoundException("No matching type registered");
    }
    List<String> fieldNames = serverTypeMap.get(typedStruct.getTypeId());
    if (fieldNames.size() != typedStruct.getFieldsCount()) {
      throw new IOException("Incorrect number of fields for encoded type");
    }
    final Struct.Builder builder = Struct.newBuilder();
    Iterator<String> keys = fieldNames.iterator();
    Iterator<Value> values = typedStruct.getFieldsList().iterator();
    while (keys.hasNext() && values.hasNext()) {
      final Value nextVal = values.next();
      if (nextVal.hasTypedStructValue()) {
        builder.putFields(keys.next(), Value.newBuilder()
            .setStructValue(typedStructToStruct(nextVal.getTypedStructValue())).build());
      } else {
        builder.putFields(keys.next(), nextVal);
      }
    }
    return builder.build();
  }
}
