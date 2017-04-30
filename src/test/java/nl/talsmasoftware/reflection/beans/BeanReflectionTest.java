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
package nl.talsmasoftware.reflection.beans;

import nl.talsmasoftware.test.TestUtil;
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
    public void testUnsupportedConstructor() {
        TestUtil.assertUnsupportedConstructor(BeanReflection.class);
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

    /**
     * Tests whether nested properties can also be accessed.
     */
    @Test
    public void testGetNestedPropertyValues() {
        NestedProperties obj = new NestedProperties(
                "level 0", new NestedProperties(
                "level 1", new NestedProperties(
                "level 2", new NestedProperties(
                "level 3", new NestedProperties()
        ))));
        assertThat(BeanReflection.getPropertyValue(obj, "value"), is((Object) "level 0"));
        assertThat(BeanReflection.getPropertyValue(obj, "nested.value"), is((Object) "level 1"));
        assertThat(BeanReflection.getPropertyValue(obj, "nested.nested.value"), is((Object) "level 2"));
        assertThat(BeanReflection.getPropertyValue(obj, "nested.nested.nested.value"), is((Object) "level 3"));
    }

    /**
     * Tests whether nested properties can also be changed.
     */
    @Test
    public void testSetNestedPropertyValues() {
        NestedProperties obj = new NestedProperties(
                "level 0", new NestedProperties(
                "level 1", new NestedProperties(
                "level 2", new NestedProperties(
                "level 3", new NestedProperties()
        ))));
        BeanReflection.setPropertyValue(obj, "value", "changed 0");
        BeanReflection.setPropertyValue(obj, "nested.value", "changed 1");
        BeanReflection.setPropertyValue(obj, "nested.nested.value", "changed 2");
        BeanReflection.setPropertyValue(obj, "nested.nested.nested.value", "changed 3");

        assertThat(obj.value, is("changed 0"));
        assertThat(obj.nested.value, is("changed 1"));
        assertThat(obj.nested.nested.value, is("changed 2"));
        assertThat(obj.nested.nested.nested.value, is("changed 3"));
    }

    @Test
    public void testGetArrayIndex() {
        BeanWithArray bean = new BeanWithArray("element 0", "element 1");
        assertThat(BeanReflection.getPropertyValue(bean, "array[0]"), is((Object) "element 0"));
        assertThat(BeanReflection.getPropertyValue(bean, "array.1"), is((Object) "element 1"));
        bean = new BeanWithArray(1, 2, 3);
        assertThat(BeanReflection.getPropertyValue(bean, "array.1"), is((Object) 2));
        assertThat(BeanReflection.getPropertyValue(bean, "array.3"), is(nullValue()));
    }

    @Test
    public void testSetArrayIndex() {
        BeanWithArray bean = new BeanWithArray("element 0", "element 1");
        assertThat(BeanReflection.setPropertyValue(bean, "array.0", "new value"), is(true));
        assertThat(((Object[]) bean.array)[0], is((Object) "new value"));
        bean = new BeanWithArray(1, 2, 3);
        assertThat(BeanReflection.setPropertyValue(bean, "array[2]", 4), is(true));
        assertThat(((int[]) bean.array)[2], is(4));
    }

    @Test
    public void testGetIterableIndex() {
        BeanWithIterable bean = new BeanWithIterable("element 0", "element 1");
        assertThat(BeanReflection.getPropertyValue(bean, "iterable[0]"), is((Object) "element 0"));
        assertThat(BeanReflection.getPropertyValue(bean, "iterable.1"), is((Object) "element 1"));
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

    public static class NestedProperties {
        public String value;
        public NestedProperties nested;

        public NestedProperties() {
            this(null, null);
        }

        public NestedProperties(String value, NestedProperties nested) {
            this.value = value;
            this.nested = nested;
        }
    }

    public static class BeanWithArray {
        public Object array;

        public BeanWithArray(String... content) {
            this.array = content;
        }

        public BeanWithArray(int... content) {
            this.array = content;
        }
    }

    public static class BeanWithIterable {
        public Iterable<Object> iterable;

        public BeanWithIterable(Object... content) {
            this.iterable = new LinkedHashSet<Object>(Arrays.asList(content));
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
