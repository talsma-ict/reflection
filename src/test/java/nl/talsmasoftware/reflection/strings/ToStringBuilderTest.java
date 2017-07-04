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
import nl.talsmasoftware.test.TestUtil;
import org.junit.Test;

import static nl.talsmasoftware.test.TestUtil.fail;
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
        assertThat(new ToStringBuilder("Plain string value"), is(notNullValue()));
        assertThat(new ToStringBuilder("Plain string value"), hasToString(equalTo("Plain string value")));
        assertThat(new ToStringBuilder(""), hasToString(equalTo(""))); // Lege prefix
        assertThat(new ToStringBuilder("Prefix").append("key", "value"), hasToString(equalTo("Prefix{key=\"value\"}")));
    }

    @Test
    public void testEmptyObject() {
        assertThat(new ToStringBuilder(getClass()), hasToString(equalTo("ToStringBuilderTest")));
        assertThat(new ToStringBuilder(getClass()).brackets("(", ")"),
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
        assertThat(new ToStringBuilder(getClass()).appendSuper("AnObject{fieldA=\"a value\", fieldB=10}").append(13),
                hasToString(equalTo("ToStringBuilderTest{fieldA=\"a value\", fieldB=10, 13}")));
        assertThat(new ToStringBuilder(this).brackets("{", "]").appendSuper("AnObject{fieldA=\"a value\"}"),
                hasToString(equalTo("ToStringBuilderTest{fieldA=\"a value\"}]")));
        assertThat(new ToStringBuilder(getClass()).brackets("[", "}").appendSuper("AnObject{fieldA=\"a value\"}"),
                hasToString(equalTo("ToStringBuilderTest[AnObject{fieldA=\"a value\"}}")));
        assertThat(new ToStringBuilder(this).brackets("[", "]").appendSuper("AnObject{fieldA=\"a value\"}"),
                hasToString(equalTo("ToStringBuilderTest[AnObject{fieldA=\"a value\"}]")));
        assertThat(new ToStringBuilder(getClass()).brackets("[", "]").appendSuper("AnObject[fieldA=\"a value\"]"),
                hasToString(equalTo("ToStringBuilderTest[fieldA=\"a value\"]")));
        // Different brackets, but a een ToStringBuilder.
        assertThat(new ToStringBuilder(this).brackets("[", "]").appendSuper(
                new ToStringBuilder("AnObject").brackets("{", "}").append("fieldA", "a value")),
                hasToString(equalTo("ToStringBuilderTest[fieldA=\"a value\"]")));
    }

    @Test
    public void testDifferentSeparator() {
        final String expected = "ToStringBuilderTest{fieldA=\"a value\", fieldB=13 & fieldC=false}";
        assertThat(new ToStringBuilder(getClass())
                        .append("fieldA", "a value")
                        .append("fieldB", 13)
                        .separator(" & ")
                        .append("fieldC", false),
                hasToString(equalTo(expected)));
    }

    @Test
    public void testCharSequenceMethods() {
        String expected = "ToStringBuilderTest{fieldA=\"a value\", fieldB=13 & fieldC=false}";
        ToStringBuilder builder = new ToStringBuilder(getClass())
                .append("fieldA", "a value")
                .append("fieldB", 13)
                .separator(" & ")
                .append("fieldC", false);
        int length = builder.length();
        assertThat("length", length, is(equalTo(expected.length())));
        for (int i = 0; i < length; i++) {
            assertThat("character " + i, builder.charAt(i), is(equalTo(expected.charAt(i))));
        }

        // Empty builder.
        expected = "ToStringBuilderTest";
        builder = new ToStringBuilder(getClass()).brackets("(", ")").includeNulls();
        length = builder.length();
        assertThat("length", length, is(equalTo(expected.length())));
        for (int i = 0; i < length; i++) {
            assertThat("character " + i, builder.charAt(i), is(equalTo(expected.charAt(i))));
        }

        // Empty builder with brackets.
        expected = "ToStringBuilderTest()";
        builder = new ToStringBuilder(getClass()).forceBrackets("(", ")").includeNulls();
        length = builder.length();
        assertThat("length", length, is(equalTo(expected.length())));
        for (int i = 0; i < length; i++) {
            assertThat("character " + i, builder.charAt(i), is(equalTo(expected.charAt(i))));
        }
    }

    @Test
    public void testCharAt() {
        ToStringBuilder subject = new ToStringBuilder("prefix").append("string", 1, 5);
        assertThat(subject, hasToString("prefix{\"trin\"}")); // for reference
        try { // negative index
            subject.charAt(-1);
            fail("Index exception expected.");
        } catch (StringIndexOutOfBoundsException expected) {
            assertThat(expected.getMessage(), containsString("-1"));
        }
        try { // too big index
            subject.charAt(14);
            fail("Index exception expected.");
        } catch (StringIndexOutOfBoundsException expected) {
            assertThat(expected.getMessage(), containsString("14"));
        }
        // test corner-cases of separate parts.
        assertThat(subject.charAt(0), is('p')); // prefix
        assertThat(subject.charAt(5), is('x'));
        assertThat(subject.charAt(6), is('{')); // leftbracket
        assertThat(subject.charAt(7), is('\"')); // body
        assertThat(subject.charAt(12), is('\"'));
        assertThat(subject.charAt(13), is('}')); // rightbracket

        // Test StringBuilder consisting of only a single random string of 25 characters.
        final String randomString = TestUtil.randomString(25, 25);
        subject = new ToStringBuilder(randomString);
        assertThat(subject.length(), is(25));
        for (int i = 0; i < 25; i++) {
            assertThat("Character " + i, subject.charAt(i), is(randomString.charAt(i)));
        }
    }

    @Test
    public void testSubSequence() {
        ToStringBuilder subject = new ToStringBuilder("");
        assertThat(subject.length(), is(0));
        assertThat(subject, hasToString(""));
        assertThat(subject.subSequence(0, 0), hasToString(""));
        subject.append(' ');
        assertThat(subject.length(), is(3));
        assertThat(subject.subSequence(0, 0), hasToString(""));
        assertThat(subject.subSequence(0, 1), hasToString("{"));
        assertThat(subject.subSequence(1, 2), hasToString(" "));
        assertThat(subject.subSequence(0, 3), hasToString("{ }"));
        assertThat(subject.subSequence(3, 3), hasToString(""));
    }

    @Test
    public void testReflect_null() {
        assertThat(ToStringBuilder.reflect(null), is(notNullValue()));
        assertThat(ToStringBuilder.reflect(null), hasToString(""));
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
        assertThat(ToStringBuilder.reflect(new JavaBean("a value", null, null, null)),
                hasToString(equalTo("JavaBean{value1=\"a value\"}")));
        assertThat(ToStringBuilder.reflect(new JavaBean(null, 42, null, null)),
                hasToString(equalTo("JavaBean{value2=42}")));
        assertThat(ToStringBuilder.reflect(new JavaBean(null, null, false, null)),
                hasToString(equalTo("JavaBean{value3=false}")));
        assertThat(ToStringBuilder.reflect(new JavaBean(null, null, null, new Object())),
                hasToString(equalTo("JavaBean{value4=Object}")));
        assertThat(ToStringBuilder.reflect(new JavaBean("a value", 42, true, new JavaBean(null, 19, null, null))),
                hasToString(equalTo("JavaBean{value1=\"a value\", value2=42, value3=true, value4=JavaBean{value2=19}}")));
    }

    @Test
    public void testReflect_CircularReference() {
        final JavaBean bean1 = new JavaBean("a value", null, null, null);
        final JavaBean bean2 = new JavaBean(null, 42, null, bean1);
        String bean2ref = JavaBean.class.getName() + "@" + Integer.toHexString(bean2.hashCode()); // default toString
        bean1.setValue4(bean2); // create circle: bean1 -> bean2 -> bean1

        assertThat(ToStringBuilder.reflect(bean1),
                hasToString(equalTo("JavaBean{value1=\"a value\", value4=JavaBean{value2=42, " +
                        "value4=JavaBean{value1=\"a value\", value4=" + bean2ref + "}}}")));
    }

}
