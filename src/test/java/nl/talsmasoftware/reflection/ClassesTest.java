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
package nl.talsmasoftware.reflection;

import nl.talsmasoftware.reflection.errorhandling.InstantiatingException;
import nl.talsmasoftware.reflection.errorhandling.MissingClassException;
import nl.talsmasoftware.test.TestUtil;
import org.junit.Test;

import static nl.talsmasoftware.test.TestUtil.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Sjoerd Talsma
 */
public class ClassesTest {
    private static final String NONEXISTENT_TYPE = "nl.talsmasoftware.reflection.NonExistentType";

    @Test
    public void testUnsupportedConstructor() {
        TestUtil.assertUnsupportedConstructor(Classes.class);
    }

    @Test
    public void testGetClass() {
        assertThat(Classes.getClass(String.class.getName()), is(equalTo((Class) String.class)));
    }

    @Test(expected = MissingClassException.class)
    public void testGetClass_NameNull() {
        Classes.getClass(null);
    }

    @Test(expected = MissingClassException.class)
    public void testGetClass_NotFound() {
        Classes.getClass(NONEXISTENT_TYPE);
    }

    @Test
    public void testGetClasses() {
        assertThat(Classes.getClasses((String[]) null), is(equalTo(new Class[0])));
        assertThat(Classes.getClasses(), is(equalTo(new Class[0])));
        assertThat(Classes.getClasses(String.class.getName()), is(equalTo(new Class[]{String.class})));
        assertThat(Classes.getClasses(Integer.class.getName(), String.class.getName()),
                is(equalTo(new Class[]{Integer.class, String.class})));
    }

    @Test(expected = MissingClassException.class)
    public void testGetClasses_NotFound() {
        Classes.getClasses(String.class.getName(), NONEXISTENT_TYPE, Integer.class.getName());
    }

    @Test
    public void testFindClass() {
        assertThat(Classes.findClass(Boolean.class.getName()), is(equalTo((Class) Boolean.class)));
    }

    @Test
    public void testFindClass_NameNull() {
        assertThat(Classes.findClass(null), is(nullValue()));
    }

    @Test
    public void testFindClass_NotFound() {
        assertThat(Classes.findClass(NONEXISTENT_TYPE), is(nullValue()));
    }

    @Test
    public void testFindClasses() {
        assertThat(Classes.findClasses((String[]) null), is(equalTo(new Class[0])));
        assertThat(Classes.findClasses(), is(equalTo(new Class[0])));
        assertThat(Classes.findClasses(String.class.getName(), NONEXISTENT_TYPE, Integer.class.getName()),
                is(equalTo(new Class[]{String.class, null, Integer.class})));
    }

    @Test
    public void testCreateNew() {
        assertThat(Classes.createNew("java.lang.Object"), is(notNullValue()));
        assertThat(Classes.createNew(Object.class), is(notNullValue()));
    }

    @Test
    public void testTryCreateNew() {
        assertThat(Classes.tryCreateNew("java.lang.Object"), is(notNullValue()));
        assertThat(Classes.tryCreateNew(Object.class), is(notNullValue()));
    }

    @Test(expected = InstantiatingException.class)
    public void testCreateNew_exceptionInConstructor() {
        Classes.createNew(Thrower.class);
        fail("InstantiatingException expected.");
    }

    @Test
    public void testTryCreateNew_exceptionInConstructor() {
        assertThat(Classes.tryCreateNew(Thrower.class), is(nullValue()));
    }

    public static class Thrower {
        public Thrower() {
            throw new IllegalStateException("Exception in constructor.");
        }
    }
}
