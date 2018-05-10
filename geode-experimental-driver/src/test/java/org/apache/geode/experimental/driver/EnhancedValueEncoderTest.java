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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.NullValue;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.protocol.protobuf.v1.BasicTypes;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class EnhancedValueEncoderTest {
  EnhancedValueEncoder encoder = new EnhancedValueEncoder();

  @Test
  public void serializeByte() {
    Byte testVal = new Byte((byte) 32);
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals((byte) testVal, (byte) value.getByte());
  }

  @Test
  public void serializeBoolean() {
    Boolean testVal = new Boolean(true);
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(testVal, value.getBoolean());
  }

  @Test
  public void serializeCharacter() {
    Character testVal = new Character('a');
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals((char) testVal, (char) value.getCharacter());
  }

  @Test
  public void serializeLong() {
    Long testVal = new Long(42352345);
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals((long) testVal, value.getLong());
  }

  @Test
  public void serializeShort() {
    Short testVal = new Short((short) 200);
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals((short) testVal, value.getShort());
  }

  @Test
  public void serializeInteger() {
    Integer testVal = 200;
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals((int) testVal, value.getInteger());
  }

  @Test
  public void serializeString() {
    String testVal = "a big ol' batch of words";
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(testVal, value.getString());
  }

  @Test
  public void serializeFloat() {
    Float testVal = (float) 200.234;
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(testVal, value.getFloat(), 0);
  }

  @Test
  public void serializeDouble() {
    Double testVal = 200.45645643523453;
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(testVal, value.getDouble(), 0);
  }

  @Test
  public void serializeDate() {
    Date testVal = new Date();
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(testVal, new Date(value.getDate()));
  }

  @Test
  public void serializeByteArray() {
    byte[] testVal = {1, 2, 3};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertArrayEquals(testVal, value.getByteArray().toByteArray());
  }

  @Test
  public void serializeCharacterArray() {
    char[] testVal = {'a', 'e', '?', '7', '[', 'P'};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getCharArray().getValues(i));
    }
  }

  @Test
  public void serializeShortArray() {
    short[] testVal = {4, 56, 54, 6, 8, 89, 12, 123, 58};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getShortArray().getValues(i));
    }
  }

  @Test
  public void serializeIntArray() {
    int[] testVal = {45654, 45, 65, 8, 68, 465684165, 5648476, 546847841};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getIntArray().getValues(i));
    }
  }

  @Test
  public void serializeLongArray() {
    long[] testVal = {45648465, 6168451364581L, 1515846515848L, 6151648166L, 506136521489L};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getLongArray().getValues(i));
    }
  }

  @Test
  public void serializeFloatArray() {
    float[] testVal = {454.85f, 61685.456f, 3468.4f, 153484.561f, 465456.45136f, 151.5848f,
        6151.64f, 50.61f, 365.21489f};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getFloatArray().getValues(i), 0);
    }
  }

  @Test
  public void serializeDoubleArray() {
    double[] testVal = {454.85, 616844.5485489653, 4565.457856786, 3468.478978, 15334563484.567891,
        46545364536.45136, 151.5848, 615178978.64, 50.67898791, 365.217897489};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getDoubleArray().getValues(i), 0);
    }
  }

  @Test
  public void serializeStringArray() {
    String[] testVal =
        {"klasnf;ea", "l;kwafneafeio", "ioewnnvioew", "895t5890", ":M<OPJ()#JGIOVHJ#VBE#TY"};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getStringArray().getValues(i));
    }
  }

  @Test
  public void serializeBooleanArray() {
    boolean[] testVal = {true, true, false};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getBooleanArray().getValues(i) == 0 ? false : true);
    }
  }

  @Test
  public void serializeByteArrayArray() {
    byte[][] testVal = {{1, 2, 3}, {54, 45, 45}, {}, {2, 43, 123, 12, 54, 3, 6, 7, 8, 9, 10}};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertArrayEquals(testVal[i], value.getByteArrayArray().getValues(i).toByteArray());
    }
  }

  @Test
  public void serializeNull() {
    Object testVal = null;
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(BasicTypes.EnhancedEncodedValue.ValueCase.NULL, value.getValueCase());
    assertEquals(NullValue.NULL_VALUE, value.getNull());
  }

  @Test
  public void serializeMap() {
    Map<Object, Object> testVal = new HashMap<>();
    testVal.put("hi", "bye");
    testVal.put("foo", "baz");
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    final List<BasicTypes.EnhancedEncodedValue> keysList = value.getMap().getKeysList();
    final List<BasicTypes.EnhancedEncodedValue> valuesList = value.getMap().getValuesList();
    assertEquals(testVal.size(), keysList.size());
    assertEquals(keysList.size(), valuesList.size());
    for (int i = 0; i < keysList.size(); ++i) {
      final String key = keysList.get(i).getString();
      assertTrue(testVal.containsKey(key));
      assertEquals(testVal.get(key), valuesList.get(i).getString());
    }
  }

  @Test
  public void serializeList() {
    List<Object> testVal = new ArrayList<>();
    testVal.add("hi");
    testVal.add("yo");
    testVal.add("howdy");
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    final List<BasicTypes.EnhancedEncodedValue> list = value.getList().getValuesList();
    assertEquals(testVal.size(), list.size());

    for (int i = 0; i < list.size(); ++i) {
      assertEquals(testVal.get(i), list.get(i).getString());
    }
  }

  @Test
  public void serializeSet() {
    Set<Object> testVal = new HashSet<>();
    testVal.add("hi");
    testVal.add("yo");
    testVal.add("howdy");
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    assertEquals(testVal.size(), value.getSet().getValuesCount());

    Set<Object> resultChecker = new HashSet<>(testVal);
    for (int i = 0; i < testVal.size(); ++i) {
      final String entry = value.getSet().getValues(i).getString();
      assertTrue(resultChecker.contains(entry));
      resultChecker.remove(entry);
    }
  }

  public static class TestClass {
    int i;
    boolean b;
    String s;
    public TestClass nester;

    public TestClass(int i, boolean b, String s) {
      this.i = i;
      this.b = b;
      this.s = s;
      nester = null;
    }

    public TestClass() {}
  }

  @Test
  public void serializeObject() throws Exception {
    final TestClass thing = new TestClass(2, false, "hello");
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(thing);

    assertEquals("i", value.getObject().getFieldNames(0));
    assertEquals("b", value.getObject().getFieldNames(1));
    assertEquals("s", value.getObject().getFieldNames(2));
    assertEquals("nester", value.getObject().getFieldNames(3));
    assertEquals(2, value.getObject().getFieldValues(0).getInteger());
    assertEquals(false, value.getObject().getFieldValues(1).getBoolean());
    assertEquals("hello", value.getObject().getFieldValues(2).getString());
    assertEquals(NullValue.NULL_VALUE, value.getObject().getFieldValues(2).getNull());

    Object decodedValue = encoder.decodeValue(value);
    assertTrue(decodedValue instanceof TestClass);
    assertTrue(decodedValue instanceof Map);
    Map<String, Object> decodedMap = (Map<String, Object>) decodedValue;
    assertEquals(thing.i, decodedMap.get("i"));
    assertEquals(thing.b, decodedMap.get("b"));
    assertEquals(thing.s, decodedMap.get("s"));
    assertEquals(null, decodedMap.get("nester"));
  }

  @Test
  public void serializeSimpleObjectArray() {
    Object[] testVal = {new Integer(354), new Float(435.234), new String("words")};
    final BasicTypes.EnhancedEncodedValue value = encoder.encodeValue(testVal);
    for (int i = 0; i < testVal.length; ++i) {
      assertEquals(testVal[i], value.getObjectArray().getValues(i));
    }
  }



}
