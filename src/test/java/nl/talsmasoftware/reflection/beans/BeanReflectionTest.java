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

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.*;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BeanReflectionTest {

    @Before
    @After
    public void clearCaches() {
        BeanReflection.flushCaches();
    }

    @Test
    public void testGetPropertyValue_nulls() {
        assertThat(BeanReflection.getPropertyValue(null, null), is(nullValue()));
        assertThat(BeanReflection.getPropertyValue(null, "property"), is(nullValue()));
        assertThat(BeanReflection.getPropertyValue(new Object(), null), is(nullValue()));
        assertThat(BeanReflection.getPropertyValue(new Object(), ""), is(nullValue()));
    }

    @Test
    public void testGetPropertyValue_notFound() {
        assertThat(BeanReflection.getPropertyValue(new Object(), "notFound"), is(nullValue()));
    }

    @Test
    public void testGetPropertyValue() {
        assertThat(BeanReflection.getPropertyValue(new Object(), "class"), is(equalTo((Object) Object.class)));
    }

    @Test
    public void testSetPropertyValue_class() {
        assertThat(BeanReflection.setPropertyValue(new Object(), "class", String.class), is(false));
    }

    @Test
    public void testGetPropertyValue_withGetter() {
        assertThat(BeanReflection.getPropertyValue(new BeanWithGetter("a"), "value"), is(equalTo((Object) "a")));
    }

    @Test
    public void testGetPropertyValue_booleanProperty() {
        assertThat(BeanReflection.getPropertyValue(new BeanWithBooleanProperty(true), "indication"), is(equalTo((Object) true)));
    }

    @Test
    public void testGetPropertyValue_accessorsAndFields() {
        BeanWithAccessorsAndFields bean = new BeanWithAccessorsAndFields("a", true);
        assertThat(BeanReflection.getPropertyValue(bean, "value"), is(equalTo((Object) "a")));
        assertThat(BeanReflection.getPropertyValue(bean, "indication"), is(equalTo((Object) true)));
        assertThat(BeanReflection.getPropertyValue(bean, "value2"), is(equalTo((Object) "a")));
        assertThat(BeanReflection.getPropertyValue(bean, "indication2"), is(equalTo((Object) true)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBeanProperties_mutability() {
        Iterator<BeanProperty> it = BeanReflection.getBeanProperties(BeanWithAccessorsAndFields.class).iterator();
        it.next();
        it.remove();
    }

    static BeanProperty find(Iterable<? extends BeanProperty> props, String name) {
        BeanProperty found = null;
        if (props != null && name != null) for (BeanProperty prop : props) {
            if (name.equals(prop.getName())) {
                found = prop;
                break;
            }
        }
        return found;
    }

    @Test
    public void testBeanPropertyAnnotations() {
        Collection<BeanProperty> props = BeanReflection.getBeanProperties(SubClass.class);
        Collection<Annotation> annotations = find(props, "value").annotations();
        assertThat(annotations, hasItem(Matchers.<Annotation>instanceOf(Readable.class)));
        annotations = find(props, "value2").annotations();
        assertThat(annotations, hasItem(Matchers.<Annotation>instanceOf(Readable.class)));
        annotations = find(props, "value3").annotations();
        assertThat(annotations, hasItem(Matchers.<Annotation>instanceOf(Readable.class)));
    }

    @Test
    public void testGetPropertyValues_null() {
        Map<String, Object> expected = Collections.emptyMap();
        assertThat(BeanReflection.getPropertyValues(null), is(equalTo(expected)));
    }

    @Test
    public void testGetPropertyValues() {
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("class", Object.class);
        assertThat("Properties of new Object", BeanReflection.getPropertyValues(new Object()), is(equalTo(expected)));

        assertThat(BeanReflection.getPropertyValues("String value").entrySet(), hasSize(3));
        assertThat(BeanReflection.getPropertyValues("String value").get("class"), is(equalTo((Object) String.class)));
        assertThat(BeanReflection.getPropertyValues("String value").get("empty"), is(equalTo((Object) Boolean.FALSE)));
        assertThat(BeanReflection.getPropertyValues("String value").get("bytes"), is(instanceOf(byte[].class)));

        expected.clear();
        expected.put("class", BeanWithGetter.class);
        expected.put("value", "val");
        assertThat("Properties of BeanWithGetter",
                BeanReflection.getPropertyValues(new BeanWithGetter("val")), is(equalTo(expected)));

        expected.clear();
        expected.put("class", BeanWithBooleanProperty.class);
        expected.put("indication", Boolean.FALSE);
        assertThat("Properties of BeanWithBooleanProperty",
                BeanReflection.getPropertyValues(new BeanWithBooleanProperty(false)), is(equalTo(expected)));

        expected.clear();
        expected.put("class", BeanWithAccessorsAndFields.class);
        expected.put("indication", Boolean.TRUE);
        expected.put("indication2", Boolean.TRUE);
        expected.put("value", "String value");
        expected.put("value2", "String value");
        assertThat("Properties of BeanWithAccessorsAndFields",
                BeanReflection.getPropertyValues(new BeanWithAccessorsAndFields("String value", true)), is(equalTo(expected)));
    }

    @Test
    public void testSetPropertyValue_finalField() {
        BeanWithAccessorsAndFields bean = new BeanWithAccessorsAndFields("old value", false);
        assertThat(BeanReflection.setPropertyValue(bean, "value", "new value"), is(equalTo(false)));
        assertThat(bean.value, is(equalTo("old value")));
    }

    @Test
    public void testSubclassReflection() {
        Map<String, Object> properties = BeanReflection.getPropertyValues(new SubClass("some value", true));
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

        @Readable
        public String getValue() {
            return value;
        }
    }

    public static class BeanWithBooleanProperty {
        private final boolean indication;

        public BeanWithBooleanProperty(boolean indication) {
            this.indication = indication;
        }

        @Readable
        public boolean isIndication() {
            return indication;
        }
    }

    public static class BeanWithAccessorsAndFields {
        @Readable
        public final String value;
        @Readable
        public final boolean indication;

        public BeanWithAccessorsAndFields(String value, boolean indication) {
            this.value = value;
            this.indication = indication;
        }

        @Readable
        public String getValue2() {
            return value;
        }

        @Readable
        public boolean isIndication2() {
            return indication;
        }
    }

    public static class SubClass extends BeanWithAccessorsAndFields {
        @Readable
        public final String value3;

        public SubClass(String value, boolean indication) {
            super(value, indication);
            this.value3 = value;
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Readable {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Writeable {

    }
}
