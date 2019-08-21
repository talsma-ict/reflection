/*
 * Copyright 2016-2019 Talsma ICT
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

import nl.talsmasoftware.reflection.errorhandling.MissingConstructorException;
import nl.talsmasoftware.test.TestUtil;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * @author Sjoerd Talsma
 */
public class ConstructorsTest {

    @Test
    public void testUnsupportedConstructor() {
        TestUtil.assertUnsupportedConstructor(Constructors.class);
    }

    @Test
    public void testGetConstructor_nullTest() {
        try {
            Constructors.getConstructor((Class) null);
            fail("Exception expected");
        } catch (MissingConstructorException expected) {
            TestUtil.assertExceptionMessage(expected);
        }
        try {
            Constructors.getConstructor((String) null);
            fail("Exception expected");
        } catch (MissingConstructorException expected) {
            TestUtil.assertExceptionMessage(expected);
        }
    }

    @Test
    public void testFindConstructor_nullTest() {
        assertThat(Constructors.findConstructor((Class) null), is(nullValue()));
        assertThat(Constructors.findConstructor((String) null), is(nullValue()));
    }

}
