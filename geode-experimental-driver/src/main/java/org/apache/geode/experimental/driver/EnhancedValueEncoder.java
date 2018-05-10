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


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.protobuf.ByteString;
import com.google.protobuf.NullValue;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.internal.protocol.protobuf.v1.BasicTypes;


/**
 * Encodes and decodes Java objects to and from Protobuf encoded values.
 *
 * <strong>This code is an experimental prototype and is presented "as is" with no warranty,
 * suitability, or fitness of purpose implied.</strong>
 */
@Experimental
class EnhancedValueEncoder {
  private Map<Class, BiConsumer<BasicTypes.EnhancedEncodedValue.Builder, Object>> classConsumerMap;
  {
    classConsumerMap = new HashMap<>();
    classConsumerMap.put(Byte.class, (builder, value) -> builder.setByte((Byte) value));
    classConsumerMap.put(Boolean.class, (builder, value) -> builder.setBoolean((Boolean) value));
    classConsumerMap.put(Character.class,
        (builder, value) -> builder.setCharacter((Character) value));
    classConsumerMap.put(Short.class, (builder, value) -> builder.setShort((Short) value));
    classConsumerMap.put(Integer.class, (builder, value) -> builder.setInteger((Integer) value));
    classConsumerMap.put(Long.class, (builder, value) -> builder.setLong((Long) value));
    classConsumerMap.put(Float.class, (builder, value) -> builder.setFloat((Float) value));
    classConsumerMap.put(Double.class, (builder, value) -> builder.setDouble((Double) value));
    classConsumerMap.put(String.class, (builder, value) -> builder.setString((String) value));
    classConsumerMap.put(Date.class, (builder, value) -> builder.setDate(((Date) value).getTime()));
    classConsumerMap.put(byte.class, (builder, value) -> builder.setByte((byte) value));
    classConsumerMap.put(boolean.class, (builder, value) -> builder.setBoolean((boolean) value));
    classConsumerMap.put(char.class, (builder, value) -> builder.setCharacter((char) value));
    classConsumerMap.put(short.class, (builder, value) -> builder.setShort((short) value));
    classConsumerMap.put(int.class, (builder, value) -> builder.setInteger((int) value));
    classConsumerMap.put(long.class, (builder, value) -> builder.setLong((long) value));
    classConsumerMap.put(float.class, (builder, value) -> builder.setFloat((float) value));
    classConsumerMap.put(double.class, (builder, value) -> builder.setDouble((double) value));
  }

  private static void handlePrimitiveArray(BasicTypes.EnhancedEncodedValue.Builder builder,
      Object primitiveArray) {
    // This would be a beautiful function to implement with generics, if only java generics were
    // capable of dealing with primitives
    Class primitiveType = primitiveArray.getClass().getComponentType();
    if (primitiveType.equals(boolean.class)) {
      final BasicTypes.VarIntArray.Builder arrayBuilder = BasicTypes.VarIntArray.newBuilder();
      for (boolean value : (boolean[]) primitiveArray) {
        arrayBuilder.addValues(value ? 1 : 0);
      }
      builder.setBooleanArray(arrayBuilder);
    } else if (primitiveType.equals(char.class)) {
      final BasicTypes.VarIntArray.Builder arrayBuilder = BasicTypes.VarIntArray.newBuilder();
      for (char value : (char[]) primitiveArray) {
        arrayBuilder.addValues(value);
      }
      builder.setCharArray(arrayBuilder);
    } else if (primitiveType.equals(short.class)) {
      final BasicTypes.VarIntArray.Builder arrayBuilder = BasicTypes.VarIntArray.newBuilder();
      for (short value : (short[]) primitiveArray) {
        arrayBuilder.addValues(value);
      }
      builder.setShortArray(arrayBuilder);
    } else if (primitiveType.equals(int.class)) {
      final BasicTypes.VarIntArray.Builder arrayBuilder = BasicTypes.VarIntArray.newBuilder();
      for (int value : (int[]) primitiveArray) {
        arrayBuilder.addValues(value);
      }
      builder.setIntArray(arrayBuilder);
    } else if (primitiveType.equals(long.class)) {
      final BasicTypes.VarIntArray.Builder arrayBuilder = BasicTypes.VarIntArray.newBuilder();
      for (long value : (long[]) primitiveArray) {
        arrayBuilder.addValues(value);
      }
      builder.setLongArray(arrayBuilder);
    } else if (primitiveType.equals(float.class)) {
      final BasicTypes.FloatArray.Builder arrayBuilder = BasicTypes.FloatArray.newBuilder();
      for (float value : (float[]) primitiveArray) {
        arrayBuilder.addValues(value);
      }
      builder.setFloatArray(arrayBuilder);
    } else if (primitiveType.equals(double.class)) {
      final BasicTypes.DoubleArray.Builder arrayBuilder = BasicTypes.DoubleArray.newBuilder();
      for (double value : (double[]) primitiveArray) {
        arrayBuilder.addValues(value);
      }
      builder.setDoubleArray(arrayBuilder);
    }
  }

  /**
   * Encodes a Java object into a Protobuf encoded value.
   *
   * @param unencodedValue Java object to encode.
   * @return Encoded value of the Java object.
   */
  BasicTypes.EnhancedEncodedValue encodeValue(Object unencodedValue) {
    BasicTypes.EnhancedEncodedValue.Builder builder = BasicTypes.EnhancedEncodedValue.newBuilder();

    if (unencodedValue == null) {
      builder.setNull(NullValue.NULL_VALUE);
    } else if (classConsumerMap.containsKey(unencodedValue.getClass())) {
      classConsumerMap.get(unencodedValue.getClass()).accept(builder, unencodedValue);
    } else if (unencodedValue.getClass().isArray()) {
      final Class componentType = unencodedValue.getClass().getComponentType();
      if (componentType.isPrimitive()) {
        if (componentType.equals(byte.class)) {
          builder.setByteArray(ByteString.copyFrom((byte[]) unencodedValue));
        } else {
          handlePrimitiveArray(builder, unencodedValue);
        }
      } else if (componentType.isArray() && componentType.getComponentType().equals(byte.class)) {
        final BasicTypes.ByteArrayArray.Builder arrayBuilder =
            BasicTypes.ByteArrayArray.newBuilder();
        for (byte[] array : (byte[][]) unencodedValue) {
          arrayBuilder.addValues(ByteString.copyFrom(array));
        }
        builder.setByteArrayArray(arrayBuilder);
      } else if (componentType.equals(String.class)) {
        builder.setStringArray(BasicTypes.StringArray.newBuilder()
            .addAllValues(Arrays.asList((String[]) unencodedValue)));
      } else {
        // object array
        final BasicTypes.ByteArrayArray.Builder arrayBuilder =
            BasicTypes.ByteArrayArray.newBuilder();
        for (Object obj : (Object[]) unencodedValue) {
          arrayBuilder.addValues(encodeValue(obj).toByteString());
        }
        builder.setObjectArray(arrayBuilder);
      }
    } else if (Map.class.isAssignableFrom(unencodedValue.getClass())) {
      final Map<?, ?> map = (Map<?, ?>) unencodedValue;

      final BasicTypes.ValueMap.Builder mapBuilder = BasicTypes.ValueMap.newBuilder();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        mapBuilder.addKeys(encodeValue(entry.getKey()));
        mapBuilder.addValues(encodeValue(entry.getValue()));
      }
      builder.setMap(mapBuilder);
    } else if (Set.class.isAssignableFrom(unencodedValue.getClass())) {
      final BasicTypes.ValueArray.Builder setBuilder = BasicTypes.ValueArray.newBuilder();
      for (Object obj : (Set<?>) unencodedValue) {
        setBuilder.addValues(encodeValue(obj));
      }
      builder.setSet(setBuilder);
    } else if (List.class.isAssignableFrom(unencodedValue.getClass())) {
      final BasicTypes.ValueArray.Builder arrayBuilder = BasicTypes.ValueArray.newBuilder();
      for (Object obj : (List<?>) unencodedValue) {
        arrayBuilder.addValues(encodeValue(obj));
      }
      builder.setList(arrayBuilder);
    } else {
      builder.setObject(autoSerializeObject(unencodedValue));
    }

    return builder.build();


    // hashmap
    // hashtable
    // arrayList
    // vector
    // hashset
    // linkedhashset
    // object


    /*
     * BasicTypes.EncodedValue.Builder builder = BasicTypes.EncodedValue.newBuilder();
     *
     * if (valueSerializer.supportsPrimitives()) {
     * ByteString customBytes = customSerialize(unencodedValue);
     * return builder.setCustomObjectResult(customBytes).build();
     * }
     *
     * if (Objects.isNull(unencodedValue)) {
     * builder.setNullResult(NullValue.NULL_VALUE);
     * } else if (Integer.class.equals(unencodedValue.getClass())) {
     * builder.setIntResult((Integer) unencodedValue);
     * } else if (Long.class.equals(unencodedValue.getClass())) {
     * builder.setLongResult((Long) unencodedValue);
     * } else if (Short.class.equals(unencodedValue.getClass())) {
     * builder.setShortResult((Short) unencodedValue);
     * } else if (Byte.class.equals(unencodedValue.getClass())) {
     * builder.setByteResult((Byte) unencodedValue);
     * } else if (Double.class.equals(unencodedValue.getClass())) {
     * builder.setDoubleResult((Double) unencodedValue);
     * } else if (Float.class.equals(unencodedValue.getClass())) {
     * builder.setFloatResult((Float) unencodedValue);
     * } else if (byte[].class.equals(unencodedValue.getClass())) {
     * builder.setBinaryResult(ByteString.copyFrom((byte[]) unencodedValue));
     * } else if (Boolean.class.equals(unencodedValue.getClass())) {
     * builder.setBooleanResult((Boolean) unencodedValue);
     * } else if (String.class.equals(unencodedValue.getClass())) {
     * builder.setStringResult((String) unencodedValue);
     * } else if (JSONWrapper.class.isAssignableFrom(unencodedValue.getClass())) {
     * builder.setJsonObjectResult(((JSONWrapper) unencodedValue).getJSON());
     * } else {
     * ByteString customBytes = customSerialize(unencodedValue);
     * if (customBytes != null) {
     * builder.setCustomObjectResult(customBytes);
     * } else {
     * throw new IllegalStateException("We don't know how to handle an object of type "
     * + unencodedValue.getClass() + ": " + unencodedValue);
     * }
     * }
     *
     * return builder.build();
     */
  }

  private BasicTypes.EncodedObject autoSerializeObject(Object unencodedValue) {
    BasicTypes.EncodedObject.Builder objectBuilder = BasicTypes.EncodedObject.newBuilder();
    for (Field field : unencodedValue.getClass().getDeclaredFields()) {
      try {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        field.setAccessible(true);
        Object value = field.get(unencodedValue);
        objectBuilder.addFieldNames(field.getName());
        objectBuilder.addFieldValues(encodeValue(value));
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    objectBuilder.setClassName(unencodedValue.getClass().getName());

    return objectBuilder.build();
  }

  /**
   * Decodes a Protobuf encoded value into a Java object.
   *
   * @param encodedValue Encoded value to decode.
   * @return Decoded Java object.
   */
  Object decodeValue(BasicTypes.EnhancedEncodedValue encodedValue) {
    switch (encodedValue.getValueCase()) {
      case BYTE:
        return (byte) encodedValue.getByte();
      case BOOLEAN:
        return encodedValue.getBoolean();
      case CHARACTER:
        return (char) encodedValue.getCharacter();
      case SHORT:
        return (short) encodedValue.getShort();
      case INTEGER:
        return encodedValue.getInteger();
      case LONG:
        return encodedValue.getLong();
      case FLOAT:
        return encodedValue.getFloat();
      case DOUBLE:
        return encodedValue.getDouble();
      case STRING:
        return encodedValue.getString();
      case DATE:
        return new Date(encodedValue.getDate());
      // ...
      case OBJECT:
        return deserializeObject(encodedValue.getObject());
      case NULL:
        return null;
      default:
        return null;
      // throw new IllegalStateException(
      // "Can't decode a value of type " + encodedValue.getValueCase() + ": " + encodedValue);
    }
  }

  Object deserializeObject(BasicTypes.EncodedObject object) {
    HashMap<String, Object> dataMap = new HashMap<>();
    final Iterator<String> keys = object.getFieldNamesList().iterator();
    final Iterator<BasicTypes.EnhancedEncodedValue>
        values =
        object.getFieldValuesList().iterator();
    while (keys.hasNext()) {
      dataMap.put(keys.next(), decodeValue(values.next()));
    }
    try {
      final Class objectClass = Class.forName(object.getClassName());

      final Object result = objectClass.newInstance();
      for (Field field : objectClass.getDeclaredFields()) {
        field.setAccessible(true);
        field.set(object, dataMap.get(field.getName()));
      }
      return result;
    } catch (ClassNotFoundException e) {
      return dataMap;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    } catch (InstantiationException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Encodes a Java object key and a Java object value into a Protobuf encoded entry.
   *
   * @param unencodedKey Java object key to encode.
   * @param unencodedValue Java object value to encode.
   * @return Encoded entry of the Java object key and value.
   */
  BasicTypes.Entry encodeEntry(Object unencodedKey, Object unencodedValue) {
    return null;
    /*
     * if (unencodedValue == null) {
     * return BasicTypes.Entry.newBuilder().setKey(encodeValue(unencodedKey)).build();
     * }
     * return BasicTypes.Entry.newBuilder().setKey(encodeValue(unencodedKey))
     * .setValue(encodeValue(unencodedValue)).build();
     */
  }
}
