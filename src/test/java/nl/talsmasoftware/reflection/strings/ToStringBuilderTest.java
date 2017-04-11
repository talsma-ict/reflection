/*
 * Copyright 2016-2017 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.reflection.strings;

import nl.talsmasoftware.reflection.JavaBean;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Sjoerd Talsma
 */
public class ToStringBuilderTest {

    @Test
    public void testConstructor_nullValue() {
        assertThat(new ToStringBuilder(null), is(notNullValue()));
        assertThat(new ToStringBuilder(null), hasToString(equalTo(""))); // empty prefix
        assertThat(new ToStringBuilder(null).append("value"), hasToString(equalTo("{\"value\"}")));
    }

    @Test
    public void testConstructor_plainString() {
        assertThat(new ToStringBuilder("Aap noot mies"), is(notNullValue()));
        assertThat(new ToStringBuilder("Aap noot mies"), hasToString(equalTo("Aap noot mies")));
        assertThat(new ToStringBuilder(""), hasToString(equalTo(""))); // Lege prefix
        assertThat(new ToStringBuilder("Naampje").append("key", "value"), hasToString(equalTo("Naampje{key=\"value\"}")));
    }

    @Test
    public void testEmptyObject() {
        assertThat(new ToStringBuilder(getClass()),
                hasToString(equalTo("ToStringBuilderTest")));
        assertThat(new ToStringBuilder(this).forceBrackets("(", ")"),
                hasToString(equalTo("ToStringBuilderTest()")));
    }

    @Test
    public void testObjectWithNullValue() {
        assertThat(new ToStringBuilder(getClass()).append(null),
                hasToString(equalTo("ToStringBuilderTest")));
        assertThat(new ToStringBuilder(this).forceBrackets("(", ")").append(null),
                hasToString(equalTo("ToStringBuilderTest()")));
        assertThat(new ToStringBuilder(getClass()).includeNulls().append(null),
                hasToString(equalTo("ToStringBuilderTest{<null>}")));
        assertThat(new ToStringBuilder(getClass()).includeNulls().append(null, null),
                hasToString(equalTo("ToStringBuilderTest{<null>}")));
        assertThat(new ToStringBuilder(getClass()).includeNulls().append("", null),
                hasToString(equalTo("ToStringBuilderTest{<null>}")));
        assertThat(new ToStringBuilder(this).includeNulls().append("name", null),
                hasToString(equalTo("ToStringBuilderTest{name=<null>}")));
    }

    @Test
    public void testObjectWithEmptyStringValue() {
        assertThat(new ToStringBuilder(getClass()).append(""),
                hasToString(equalTo("ToStringBuilderTest{\"\"}")));
        assertThat(new ToStringBuilder(this).forceBrackets("(", ")").append(""),
                hasToString(equalTo("ToStringBuilderTest(\"\")")));
        assertThat(new ToStringBuilder(getClass()).includeNulls().append(""),
                hasToString(equalTo("ToStringBuilderTest{\"\"}")));
        assertThat(new ToStringBuilder(this).includeNulls().append(null, ""),
                hasToString(equalTo("ToStringBuilderTest{\"\"}")));
        assertThat(new ToStringBuilder(getClass()).includeNulls().append("", ""),
                hasToString(equalTo("ToStringBuilderTest{\"\"}")));
        assertThat(new ToStringBuilder(this).includeNulls().append("name", ""),
                hasToString(equalTo("ToStringBuilderTest{name=\"\"}")));
    }

    @Test
    public void testAppendSuper() {
        assertThat(new ToStringBuilder(getClass()).appendSuper("EenObject{veldA=\"een waarde\", veldB=10}").append(13),
                hasToString(equalTo("ToStringBuilderTest{veldA=\"een waarde\", veldB=10, 13}")));
        assertThat(new ToStringBuilder(this).brackets("{", "]").appendSuper("EenObject{veldA=\"een waarde\"}"),
                hasToString(equalTo("ToStringBuilderTest{veldA=\"een waarde\"}]")));
        assertThat(new ToStringBuilder(getClass()).brackets("[", "}").appendSuper("EenObject{veldA=\"een waarde\"}"),
                hasToString(equalTo("ToStringBuilderTest[EenObject{veldA=\"een waarde\"}}")));
        assertThat(new ToStringBuilder(this).brackets("[", "]").appendSuper("EenObject{veldA=\"een waarde\"}"),
                hasToString(equalTo("ToStringBuilderTest[EenObject{veldA=\"een waarde\"}]")));
        assertThat(new ToStringBuilder(getClass()).brackets("[", "]").appendSuper("EenObject[veldA=\"een waarde\"]"),
                hasToString(equalTo("ToStringBuilderTest[veldA=\"een waarde\"]")));
        // Andere brackets, maar wel een ToStringBuilder.
        assertThat(new ToStringBuilder(this).brackets("[", "]").appendSuper(
                new ToStringBuilder("EenObject").brackets("{", "}").append("veldA", "een waarde")),
                hasToString(equalTo("ToStringBuilderTest[veldA=\"een waarde\"]")));
    }

    @Test
    public void testAndereSeparator() {
        final String expected = "ToStringBuilderTest{veldA=\"een waarde\", veldB=13 & veldC=false}";
        assertThat(new ToStringBuilder(getClass())
                        .append("veldA", "een waarde")
                        .append("veldB", 13)
                        .separator(" & ")
                        .append("veldC", false),
                hasToString(equalTo(expected)));
    }

    @Test
    public void testCharSequenceMethods() {
        String expected = "ToStringBuilderTest{veldA=\"een waarde\", veldB=13 & veldC=false}";
        ToStringBuilder builder = new ToStringBuilder(getClass())
                .append("veldA", "een waarde")
                .append("veldB", 13)
                .separator(" & ")
                .append("veldC", false);
        int lengte = builder.length();
        assertThat("lengte", lengte, is(equalTo(expected.length())));
        for (int i = 0; i < lengte; i++) {
            assertThat("karakter " + i, builder.charAt(i), is(equalTo(expected.charAt(i))));
        }

        // Lege builder.
        expected = "ToStringBuilderTest";
        builder = new ToStringBuilder(getClass()).brackets("(", ")").includeNulls();
        lengte = builder.length();
        assertThat("lengte", lengte, is(equalTo(expected.length())));
        for (int i = 0; i < lengte; i++) {
            assertThat("karakter " + i, builder.charAt(i), is(equalTo(expected.charAt(i))));
        }

        // Lege builder met brackets.
        expected = "ToStringBuilderTest()";
        builder = new ToStringBuilder(getClass()).forceBrackets("(", ")").includeNulls();
        lengte = builder.length();
        assertThat("lengte", lengte, is(equalTo(expected.length())));
        for (int i = 0; i < lengte; i++) {
            assertThat("karakter " + i, builder.charAt(i), is(equalTo(expected.charAt(i))));
        }
    }

    @Test
    public void testReflect_null() {
        assertThat(ToStringBuilder.reflect(null), is(nullValue()));
    }

    @Test
    public void testReflect_StringValue() {
        assertThat(ToStringBuilder.reflect("The quick brown fox jumps over the lazy dog."),
                hasToString(equalTo("The quick brown fox jumps over the lazy dog.")));
    }

    @Test
    public void testReflect_JavaObject() {
        assertThat(ToStringBuilder.reflect(new Object()), hasToString(equalTo("Object")));
    }

    @Test
    public void testReflect_JavaBean() {
        assertThat(ToStringBuilder.reflect(new JavaBean("een waarde", null, null, null)),
                hasToString(equalTo("JavaBean{value1=\"een waarde\"}")));
        assertThat(ToStringBuilder.reflect(new JavaBean(null, 42, null, null)),
                hasToString(equalTo("JavaBean{value2=42}")));
        assertThat(ToStringBuilder.reflect(new JavaBean(null, null, false, null)),
                hasToString(equalTo("JavaBean{value3=false}")));
        assertThat(ToStringBuilder.reflect(new JavaBean(null, null, null, new Object())),
                hasToString(equalTo("JavaBean{value4=Object}")));
        assertThat(ToStringBuilder.reflect(new JavaBean("een waarde", 42, true, new JavaBean(null, 19, null, null))),
                hasToString(equalTo("JavaBean{value1=\"een waarde\", value2=42, value3=true, value4=JavaBean{value2=19}}")));
    }

    @Test
    public void testReflect_CircularReference() {
        JavaBean bean1 = new JavaBean("een waarde", null, null, null);
        JavaBean bean2 = new JavaBean(null, 42, null, bean1);
        String bean2ref = JavaBean.class.getName() + "@" + Integer.toHexString(bean2.hashCode()); // default toString
        bean1.setValue4(bean2);

        assertThat(ToStringBuilder.reflect(bean1),
                hasToString(equalTo("JavaBean{value1=\"een waarde\", value4=JavaBean{value2=42, " +
                        "value4=JavaBean{value1=\"een waarde\", value4=" + bean2ref + "}}}")));
    }

}
