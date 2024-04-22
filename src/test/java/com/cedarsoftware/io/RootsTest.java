package com.cedarsoftware.io;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.DeepEquals.deepEquals;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class RootsTest
{
    @Test
    void testStringRoot()
    {
        Gson gson = new Gson();
        String g = gson.toJson("root should not be a string");
        String j = TestUtil.toJson("root should not be a string");
        assertEquals(g, j);
    }

    @Test
    void testRoots()
    {
        // Test Object[] as root element passed in
        Object[] foo = new Object[]{new TestObject("alpha"), new TestObject("beta")};

        String jsonOut = TestUtil.toJson(foo);
        TestUtil.printLine(jsonOut);

        Object[] bar = TestUtil.toObjects(jsonOut, null);
        assertEquals(2, bar.length);
        assertEquals(bar[0], new TestObject("alpha"));
        assertEquals(bar[1], new TestObject("beta"));

        String json = "[\"getStartupInfo\",[\"890.022905.16112006.00024.0067ur\",\"machine info\"]]";
        Object[] baz = TestUtil.toObjects(json, null);
        assertEquals(2, baz.length);
        assertEquals("getStartupInfo", baz[0]);
        Object[] args = (Object[]) baz[1];
        assertEquals(2, args.length);
        assertEquals("890.022905.16112006.00024.0067ur", args[0]);
        assertEquals("machine info", args[1]);

        String hw = "[\"Hello, World\"]";
        Object[] qux = TestUtil.toObjects(hw, null);
        assertNotNull(qux);
        assertEquals("Hello, World", qux[0]);

        // Whitespace
        String pkg = TestObject.class.getName();
        Object[] fred = TestUtil.toObjects("[  {  \"@type\"  :  \"" + pkg + "\"  ,  \"_name\"  :  \"alpha\"  ,  \"_other\"  :  null  }  ,  {  \"@type\"  :  \"" + pkg + "\"  ,  \"_name\"  :  \"beta\"  ,  \"_other\" : null  }  ]  ", null);
        assertNotNull(fred);
        assertEquals(2, fred.length);
        assertEquals(fred[0], (new TestObject("alpha")));
        assertEquals(fred[1], (new TestObject("beta")));

        Object[] wilma = TestUtil.toObjects("[{\"@type\":\"" + pkg + "\",\"_name\" : \"alpha\" , \"_other\":null,\"fake\":\"_typeArray\"},{\"@type\": \"" + pkg + "\",\"_name\":\"beta\",\"_other\":null}]", null);
        assertNotNull(wilma);
        assertEquals(2, wilma.length);
        assertEquals(wilma[0], (new TestObject("alpha")));
        assertEquals(wilma[1], (new TestObject("beta")));
    }

    @Test
    void testRootTypes()
    {
        assert deepEquals(25L, TestUtil.toObjects("25", null));
        assert deepEquals(25.0d, TestUtil.toObjects("25.0", null));
        assertEquals(true, TestUtil.toObjects("true", null));
        assertEquals(false, TestUtil.toObjects("false", null));
        assertEquals("foo", TestUtil.toObjects("\"foo\"", null));
    }

    @Test
    void testRoots2()
    {
        // Test root JSON type as [ ]
        Object array = new Object[]{"Hello"};
        String json = TestUtil.toJson(array);
        Object oa = TestUtil.toObjects(json, null);
        assertTrue(oa.getClass().isArray());
        assertEquals("Hello", ((Object[]) oa)[0]);

        // Test root JSON type as { }
        Calendar cal = Calendar.getInstance();
        cal.set(1965, 11, 17);
        json = TestUtil.toJson(cal);
        TestUtil.printLine("json = " + json);
        Object obj = TestUtil.toObjects(json, null);
        assertFalse(obj.getClass().isArray());
        Calendar date = (Calendar) obj;
        assertEquals(1965, date.get(Calendar.YEAR));
        assertEquals(11, date.get(Calendar.MONTH));
        assertEquals(17, date.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void testNull()
    {
        String json = TestUtil.toJson(null);
        TestUtil.printLine("json=" + json);
        assert "null".equals(json);
    }

    @Test
    void testEmptyObject()
    {
        Object o = TestUtil.toObjects("{}", null);
        assert JsonObject.class.equals(o.getClass());

        Object[] oa = TestUtil.toObjects("[{},{}]", null);
        assert oa.length == 2;
        assert JsonObject.class.equals(oa[0].getClass());
        assert JsonObject.class.equals(oa[1].getClass());
    }
    
    @Test
    void testRootConvertableNonJsonPrimitiveStaysAsJsonObject()
    {
        ZonedDateTime zdt = ZonedDateTime.parse("2024-04-22T01:34:57.170836-04:00[America/New_York]");
        String json = TestUtil.toJson(zdt, new WriteOptionsBuilder().withExtendedAliases().build());

        assertThatThrownBy(() -> { ZonedDateTime zdt2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .withExtendedAliases()
                .returnAsNativeJsonObjects()
                .build(), null);
        })
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("class com.cedarsoftware.io.JsonObject cannot be cast to class java.time.ZonedDateTime");
        
        // When forced, it's ok.
        ZonedDateTime zdt2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), ZonedDateTime.class);
        assertEquals(zdt2, zdt);

        // If you forget withExtendedAliases() and have failOnUnknownType() == true (default)
        // The following two tests work. Why?  Because although the @type is unknown, the Map has a "value" key and a
        // root type is specified (ZonedDateTime.class). The converter converts the map that has a "value" key to the
        // specified type, never looking at @type (Maps can be converted to many types via the converter.)
        assertThatThrownBy(() -> TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .build(), ZonedDateTime.class))
                .isInstanceOf(JsonIoException.class)
                .hasMessageContaining("'ZonedDateTime' not defined");
        
        // If you forget withExtendedAliases() and have .failOnUnknownType() == false,
        ZonedDateTime zdt3 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .failOnUnknownType(false)
                .build(), ZonedDateTime.class);  // Forces conversion to ZonedDateTime
        deepEquals(zdt3, zdt);
    }

    @Test
    void testRootConvertableJsonPrimitiveCousinConvertsToJavaAtomicLong()
    {
        Number number = new AtomicLong(16);
        String json = TestUtil.toJson(number, new WriteOptionsBuilder().withExtendedAliases().build());
        Object what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assert what instanceof JsonObject;
        Map<?,?> map = (Map<?,?>) what;
        assert map.get("value").equals(16L);

        // Specifying root of AtomicLong.class below "trumps" returnAsNativeJsonObjects()
        Number number2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), AtomicLong.class);
        assert deepEquals(number2, new AtomicLong(16));

        number = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), null);
        assert deepEquals(number, new AtomicLong(16));

        number = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), AtomicLong.class);
        assert deepEquals(number, new AtomicLong(16));
    }

    @Test
    void testRootConvertableJsonPrimitiveCousinConvertsToJavaByte()
    {
        Number number = Byte.valueOf("16");
        String json = TestUtil.toJson(number, new WriteOptionsBuilder().withExtendedAliases().build());
        Object what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assert what instanceof JsonObject;
        Map<?,?> map = (Map<?,?>) what;
        assert map.get("value").equals(16L);

        Number number2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), Byte.class);
        assertEquals(number2, Byte.valueOf("16"));

        number = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(number, Byte.valueOf("16"));

        number = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), Byte.class);
        assertEquals(number, Byte.valueOf("16"));
    }

    @Test
    void testRootConvertableJsonPrimitiveCousinConvertsToJavaZonedDateTime()
    {
        ZonedDateTime zdt = ZonedDateTime.now();
        String json = TestUtil.toJson(zdt, new WriteOptionsBuilder().withExtendedAliases().build());
        Object what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assert what instanceof JsonObject;
        Map<?,?> map = (Map<?,?>) what;
        assert map.get("value").equals(zdt.toString());

        ZonedDateTime zdt2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), ZonedDateTime.class);
        assertEquals(zdt2, zdt);

        zdt2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(zdt2, zdt);

        zdt2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), ZonedDateTime.class);
        assertEquals(zdt2, zdt);
    }

    @Test
    void testRootConvertableJsonPrimitiveCousinConvertsToJavaString()
    {
        String json = "{\"@type\":\"string\",\"value\":\"json-io\"}";
        Object what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(what, "json-io");

        String s2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), String.class);
        assertEquals(s2, "json-io");

        s2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(s2, "json-io");

        s2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), String.class);
        assertEquals(s2, "json-io");
    }

    @Test
    void testRootConvertableJsonPrimitiveCousinConvertsToJavaLong()
    {
        String json = "{\"@type\":\"long\",\"value\":16}";
        Object what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(what, 16L);

        json = "{\"@type\":\"long\",\"value\":\"16\"}";
        what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(what, 16L);

        json = "{\"@type\":\"long\",\"value\":\"16\"}";
        long l2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), long.class);
        assertEquals(l2, 16L);

        l2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), null);
        assertEquals(l2, 16L);

        l2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), Long.class);
        assertEquals(l2, 16L);
    }

    @Test
    void testRootConvertableJsonPrimitiveCousinConvertsToJavaStringBuilder()
    {
        String json = "{\"@type\":\"StringBuilder\",\"value\":\"json-io\"}";
        Object what = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), null);
        assert what instanceof JsonObject;
        Map<?,?> map = (Map<?,?>) what;
        assert map.get("value").equals("json-io");

        StringBuilder b2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), StringBuilder.class);
        assert deepEquals(b2, new StringBuilder("json-io"));

        String sb = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsNativeJsonObjects()
                .withExtendedAliases()
                .build(), String.class);    // String.class forces the conversion.
        assert deepEquals(sb, "json-io");

        b2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), null);
        assert deepEquals(b2, new StringBuilder("json-io"));    // respects the @type=StringBuilder

        sb = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), String.class);
        assertEquals(sb, "json-io");

        b2 = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), StringBuilder.class);                 // forced to StringBuilder and @type=StringBuilder, you get StringBuilder
        assertEquals(sb, "json-io");

        StringBuffer stringBuffer = TestUtil.toObjects(json, new ReadOptionsBuilder()
                .returnAsJavaObjects()
                .withExtendedAliases()
                .build(), StringBuffer.class);                 // forced to StringBuffer and @type=StringBuilder, you get StringBuffer
        deepEquals(stringBuffer, "json-io");
    }
}