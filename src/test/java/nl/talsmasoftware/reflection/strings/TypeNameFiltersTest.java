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

import nl.talsmasoftware.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
public class TypeNameFiltersTest {
    public static class TestFilter implements TypeNameFilter {
        public String apply(String typeName) {
            return "TypeNameFiltersTest".equals(typeName) ? typeName + "-tested" : typeName;
        }
    }

    @Before
    @After
    public void resetTypeNameFilters() {
        TypeNameFilters.reset();
        assertThat(TestUtil.getPrivateField(TypeNameFilters.class, "filter"), is(nullValue()));
    }

    @Test
    public void testUnsupportedConstructor() {
        TestUtil.assertUnsupportedConstructor(TypeNameFilters.class);
    }

    @Test
    public void testFiltered() {
        assertThat("Filter applied", TypeNameFilters.filter(getClass().getSimpleName()), is("TypeNameFiltersTest-tested"));
    }

}
