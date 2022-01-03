/*
 * Copyright 2016-2022 Talsma ICT
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
package nl.talsmasoftware.reflection;

import nl.talsmasoftware.reflection.errorhandling.MethodInvocationException;
import nl.talsmasoftware.reflection.errorhandling.MissingMethodException;
import nl.talsmasoftware.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static nl.talsmasoftware.test.TestUtil.assertExceptionMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Sjoerd Talsma
 */
public class MethodsTest {

    @Test
    public void testUnsupportedConstructor() {
        TestUtil.assertUnsupportedConstructor(Methods.class);
    }

    @Test
    public void testGetMethod() throws NoSuchMethodException {
        assertThat(Methods.getMethod(Object.class, "toString"),
                is(equalTo(Object.class.getMethod("toString"))));
        assertThat(Methods.getMethod(Object.class, "equals", Object.class.getName()),
                is(equalTo(Object.class.getMethod("equals", Object.class))));
    }

    @Test
    public void testGetMethod_typeNull() {
        try {
            Methods.getMethod((Class) null, "methodName");
            fail("Exception expected");
        } catch (MissingMethodException expected) {
            assertExceptionMessage(expected);
        }
    }

    @Test
    public void testGetMethod_nameNull() {
        try {
            Methods.getMethod(String.class, null);
            fail("Exception expected");
        } catch (MissingMethodException expected) {
            assertExceptionMessage(expected);
        }
    }

    @Test
    public void testGetMethod_QName() throws NoSuchMethodException {
        assertThat(Methods.getMethod(String.class.getName() + ".toString"),
                is(equalTo(String.class.getMethod("toString"))));
        assertThat(Methods.getMethod(String.class.getName() + ".equals", Object.class.getName()),
                is(equalTo(String.class.getMethod("equals", Object.class))));
    }

    @Test
    public void testGetMethod_QName_null() {
        try {
            Methods.getMethod((String) null);
            fail("Exception expected");
        } catch (MissingMethodException expected) {
            assertExceptionMessage(expected);
        }
    }

    @Test
    public void testGetMethod_QName_withoutClass() {
        try {
            Methods.getMethod("toString");
            fail("Exception expected");
        } catch (MissingMethodException expected) {
            assertExceptionMessage(expected);
        }
    }

    @Test
    public void testFindMethod() throws NoSuchMethodException {
        assertThat(Methods.findMethod(Object.class, "toString"),
                is(equalTo(Object.class.getMethod("toString"))));
        assertThat(Methods.findMethod(Object.class, "equals", Object.class.getName()),
                is(equalTo(Object.class.getMethod("equals", Object.class))));
    }

    @Test
    public void testFindMethod_typeNull() {
        assertThat(Methods.findMethod((Class) null, "methodName"), is(nullValue()));
    }

    @Test
    public void testFindMethod_nameNull() {
        assertThat(Methods.findMethod(String.class, null), is(nullValue()));
    }

    @Test
    public void testFindMethod_QName() throws NoSuchMethodException {
        assertThat(Methods.findMethod(String.class.getName() + ".toString"),
                is(equalTo(String.class.getMethod("toString"))));
        assertThat(Methods.findMethod(String.class.getName() + ".equals", Object.class.getName()),
                is(equalTo(String.class.getMethod("equals", Object.class))));
    }

    @Test
    public void testFindMethod_QName_null() {
        assertThat(Methods.findMethod((String) null), is(nullValue()));
    }

    @Test
    public void testFindMethod_QName_withoutClass() {
        assertThat(Methods.findMethod("toString"), is(nullValue()));
    }

    @Test
    public void testCall() throws NoSuchMethodException {
        assertThat(Methods.call(Object.class.getMethod("toString"), "The quick brown fox jumps over the lazy dog"),
                is((Object) "The quick brown fox jumps over the lazy dog"));
        assertThat(Methods.call("java.lang.Object.equals", "my value", "my value"), is((Object) true));
    }

    @Test
    public void testCall_null() {
        try {
            Methods.call((Method) null, new Object());
            fail("Exception expected.");
        } catch (MethodInvocationException expected) {
            assertExceptionMessage(expected);
        }
    }

    @Test
    public void testCall_subjectNullNonStaticMethod() throws NoSuchMethodException {
        try {
            Methods.call(Object.class.getMethod("toString"), null);
            fail("Exception expected.");
        } catch (MethodInvocationException expected) {
            assertExceptionMessage(expected);
        }
    }
}
