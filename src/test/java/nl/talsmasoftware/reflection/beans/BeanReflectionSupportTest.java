/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.reflection.beans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BeanReflectionSupportTest {

    @Before
    @After
    public void clearCaches() {
        BeanReflectionSupport.flushCaches();
    }

    @Test
    public void testGetProperty_nulls() {
        assertThat(BeanReflectionSupport.getPropertyValue(null, null), is(nullValue()));
        assertThat(BeanReflectionSupport.getPropertyValue(null, "property"), is(nullValue()));
        assertThat(BeanReflectionSupport.getPropertyValue(new Object(), null), is(nullValue()));
        assertThat(BeanReflectionSupport.getPropertyValue(new Object(), ""), is(nullValue()));
    }

    @Test
    public void testGetProperty_notFound() {
        assertThat(BeanReflectionSupport.getPropertyValue(new Object(), "notFound"), is(nullValue()));
    }

    @Test
    public void testGetter() {
        assertThat(BeanReflectionSupport.getPropertyValue(new BeanWithGetter("a"), "value"), is(equalTo((Object) "a")));
    }

    @Test
    public void testBooleanProperty() {
        assertThat(BeanReflectionSupport.getPropertyValue(new BeanWithBooleanProperty(true), "indication"), is(equalTo((Object) true)));
    }

    @Test
    public void testAccessorsAndFields() {
        BeanWithAccessorsAndFields bean = new BeanWithAccessorsAndFields("a", true);
        assertThat(BeanReflectionSupport.getPropertyValue(bean, "value"), is(equalTo((Object) "a")));
        assertThat(BeanReflectionSupport.getPropertyValue(bean, "indication"), is(equalTo((Object) true)));
        assertThat(BeanReflectionSupport.getPropertyValue(bean, "value2"), is(equalTo((Object) "a")));
        assertThat(BeanReflectionSupport.getPropertyValue(bean, "indication2"), is(equalTo((Object) true)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetProperties_mutability() {
        Iterator<BeanProperty> it = BeanReflectionSupport.getBeanProperties(BeanWithAccessorsAndFields.class).iterator();
        it.next();
        it.remove();
    }

    @Test
    public void testGetPropertyValues_null() {
        Map<String, Object> expected = Collections.emptyMap();
        assertThat(BeanReflectionSupport.getPropertyValues(null), is(equalTo(expected)));
    }

    @Test
    public void testGetPropertyValues() {
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("class", Object.class);
        assertThat("Properties of new Object", BeanReflectionSupport.getPropertyValues(new Object()), is(equalTo(expected)));

        assertThat(BeanReflectionSupport.getPropertyValues("String value").entrySet(), hasSize(3));
        assertThat(BeanReflectionSupport.getPropertyValues("String value").get("class"), is(equalTo((Object) String.class)));
        assertThat(BeanReflectionSupport.getPropertyValues("String value").get("empty"), is(equalTo((Object) Boolean.FALSE)));
        assertThat(BeanReflectionSupport.getPropertyValues("String value").get("bytes"), is(instanceOf(byte[].class)));

        expected.clear();
        expected.put("class", BeanWithGetter.class);
        expected.put("value", "val");
        assertThat("Properties of BeanWithGetter",
                BeanReflectionSupport.getPropertyValues(new BeanWithGetter("val")), is(equalTo(expected)));

        expected.clear();
        expected.put("class", BeanWithBooleanProperty.class);
        expected.put("indication", Boolean.FALSE);
        assertThat("Properties of BeanWithBooleanProperty",
                BeanReflectionSupport.getPropertyValues(new BeanWithBooleanProperty(false)), is(equalTo(expected)));

        expected.clear();
        expected.put("class", BeanWithAccessorsAndFields.class);
        expected.put("indication", Boolean.TRUE);
        expected.put("indication2", Boolean.TRUE);
        expected.put("value", "String value");
        expected.put("value2", "String value");
        assertThat("Properties of BeanWithAccessorsAndFields",
                BeanReflectionSupport.getPropertyValues(new BeanWithAccessorsAndFields("String value", true)), is(equalTo(expected)));
    }

    @Test
    public void testSetProperty_finalField() {
        BeanWithAccessorsAndFields bean = new BeanWithAccessorsAndFields("old value", false);
        assertThat(BeanReflectionSupport.setPropertyValue(bean, "value", "new value"), is(equalTo(false)));
        assertThat(bean.value, is(equalTo("old value")));
    }

    @Test
    public void testSubclassReflection() {
        Map<String, Object> properties = BeanReflectionSupport.getPropertyValues(new SubClass("some value", true));
        assertThat(properties.keySet(), containsInAnyOrder("class", "value", "value2", "value3", "indication", "indication2"));
        assertThat(properties.get("class"), equalTo((Object) SubClass.class));
        assertThat(properties.get("value"), equalTo((Object) "some value"));
        assertThat(properties.get("value2"), equalTo((Object) "some value"));
        assertThat(properties.get("value3"), equalTo((Object) "some value"));
        assertThat(properties.get("indication"), equalTo((Object) true));
        assertThat(properties.get("indication2"), equalTo((Object) true));
    }

    public static class BeanWithGetter {
        private final String value;

        public BeanWithGetter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class BeanWithBooleanProperty {
        private final boolean indication;

        public BeanWithBooleanProperty(boolean indication) {
            this.indication = indication;
        }

        public boolean isIndication() {
            return indication;
        }
    }

    public static class BeanWithAccessorsAndFields {
        public final String value;
        public final boolean indication;

        public BeanWithAccessorsAndFields(String value, boolean indication) {
            this.value = value;
            this.indication = indication;
        }

        public String getValue2() {
            return value;
        }

        public boolean isIndication2() {
            return indication;
        }
    }

    public static class SubClass extends BeanWithAccessorsAndFields {

        public final String value3;

        public SubClass(String value, boolean indication) {
            super(value, indication);
            this.value3 = value;
        }

    }

}
