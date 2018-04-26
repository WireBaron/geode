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

import static org.apache.geode.protocol.serialization.ProtobufStructSerializer.PROTOBUF_STRUCT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.NullValue;
import com.google.protobuf.UnsafeByteOperations;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.protocol.protobuf.v1.BasicTypes;
import org.apache.geode.internal.protocol.protobuf.v1.ProtobufSerializationService;
import org.apache.geode.internal.protocol.protobuf.v1.TypedStruct;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxInstanceFactory;

public class ProtobufTypedStructSerializer implements ValueSerializer {
  private Cache cache;

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
    return serializeStruct(object).toByteString();
  }

  TypedStruct serializeStruct(Object object) {
    PdxInstance pdxInstance = (PdxInstance) object;

    TypedStruct.Builder structBuilder = TypedStruct.newBuilder();
    for (String fieldName : pdxInstance.getFieldNames()) {
      Object value = pdxInstance.getField(fieldName);
      BasicTypes.EncodedValue serialized = serializeValue(value);
      structBuilder.addFields(serialized);
    }

    return structBuilder.build();
  }

  BasicTypes.EncodedValue serializeValue(Object value) {
    BasicTypes.EncodedValue.Builder builder = BasicTypes.EncodedValue.newBuilder();
    if (value instanceof String) {
      builder.setStringResult((String) value).build();
    } else if (value instanceof Boolean) {
      builder.setBooleanResult((Boolean) value).build();
    } else if (value instanceof Integer) {
      builder.setIntResult((Integer) value).build();
    } else if (value instanceof Byte) {
      builder.setByteResult((Byte) value).build();
    } else if (value instanceof Long) {
      builder.setLongResult((Long) value).build();
    } else if (value instanceof Double) {
      builder.setDoubleResult((Double) value).build();
    } else if (value instanceof byte[]) {
      builder.setBinaryResult(UnsafeByteOperations.unsafeWrap((byte[]) value)).build();
    } else if (value instanceof PdxInstance) {
      builder.setCustomObjectResult(serializeStruct(value).toByteString());
    } else if (value instanceof List) {
      throw new IllegalStateException("We don't do lists!");
    } else if (value == null) {
      builder.setNullResult(NullValue.NULL_VALUE).build();
    } else {
      throw new IllegalStateException(
          "Don't know how to translate object of type " + value.getClass() + ": " + value);
    }
    return builder.build();
  }

  @Override
  public Object deserialize(ByteString bytes) throws IOException, ClassNotFoundException {
    TypedStruct struct = TypedStruct.parseFrom(bytes);
    List<String> fieldNames = serverTypeMap.get(struct.getTypeId());
    if (fieldNames.size() != struct.getFieldsCount()) {
      throw new IOException("Incorrect number of fields for encoded type");
    }

    PdxInstanceFactory pdxInstanceFactory = cache.createPdxInstanceFactory(PROTOBUF_STRUCT);

    Iterator<String> keys = fieldNames.iterator();
    Iterator<BasicTypes.EncodedValue> values = struct.getFieldsList().iterator();
    while (keys.hasNext() && values.hasNext()) {
      final String fieldName = keys.next();
      final Object value = new ProtobufSerializationService(this).decode(values.next());

      if (value instanceof String) {
        pdxInstanceFactory.writeString(fieldName, (String) value);
      } else if (value instanceof Boolean) {
        pdxInstanceFactory.writeBoolean(fieldName, (Boolean) value);
      } else if (value instanceof Integer) {
        pdxInstanceFactory.writeInt(fieldName, (Integer) value);
      } else if (value instanceof Byte) {
        pdxInstanceFactory.writeByte(fieldName, (Byte) value);
      } else if (value instanceof Long) {
        pdxInstanceFactory.writeLong(fieldName, (Long) value);
      } else if (value instanceof byte[]) {
        pdxInstanceFactory.writeByteArray(fieldName, (byte[]) value);
      } else if (value instanceof Double) {
        pdxInstanceFactory.writeDouble(fieldName, (Double) value);
      } else if (value instanceof PdxInstance) {
        pdxInstanceFactory.writeObject(fieldName, value);
      } else if (value instanceof List) {
        pdxInstanceFactory.writeObject(fieldName, value);
      } else if (value == null) {
        pdxInstanceFactory.writeObject(fieldName, null);
      } else {
        throw new IllegalStateException(
            "Don't know how to translate object of type " + value.getClass() + ": " + value);
      }
    }

    return pdxInstanceFactory.create();
  }

  @Override
  public void init(Cache cache) {
    this.cache = cache;
  }
}
