/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.test;

import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestUtilTest {
    private static final Logger LOGGER = Logger.getLogger(TestUtilTest.class.getName());

    @Test
    public void testLogRandomString() {
        String randomString = TestUtil.randomString(12, 20);
        LOGGER.log(Level.FINE, "Generated random string from 12 to 20 characters: \"{0}\".", randomString);
        assertThat("Random string", randomString, is(notNullValue()));
        assertThat("Random string length", randomString.length(), is(greaterThan(11)));
        assertThat("Random string length", randomString.length(), is(lessThan(21)));
    }

    @Test
    public void testDefaultRandomChars() {
        assertEquals("Default random characters",
                " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
                TestUtil.getPrivateField(TestUtil.class, "DEFAULT_RANDOM_CHARS"));
    }

    @Test
    public void testAssertUnsupportedConstructor_failures() {
        for (Class<?> type : new Class[]{null, Object.class, ObjectWithDefaultConstructor.class,
                ObjectWithoutConstructorException.class, ObjectWithMultipleConstructors.class,
                ObjectWithWrongConstructorException.class}) {
            boolean assertionError = false;
            try {
                TestUtil.assertUnsupportedConstructor(type);
            } catch (AssertionError expected) {
                assertionError = true;
            }
            assertTrue("Assertion error verwacht.", assertionError);
        }
    }

    @Test
    public void testAssertUnsupportedConstructor_success() {
        // TestUtil is also a utility class itself:
        TestUtil.assertUnsupportedConstructor(TestUtil.class);
    }

    public static final class ObjectWithDefaultConstructor {
    }

    public static final class ObjectWithoutConstructorException {
        private ObjectWithoutConstructorException() {
        }
    }

    public static final class ObjectWithMultipleConstructors {
        private ObjectWithMultipleConstructors() {
        }

        private ObjectWithMultipleConstructors(int second) {
        }
    }

    public static final class ObjectWithWrongConstructorException {
        private ObjectWithWrongConstructorException() {
            throw new NullPointerException();
        }
    }
}
