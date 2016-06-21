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
 */

package nl.talsmasoftware.reflection.dto;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test DTO objects.
 *
 * @author <a href="mailto:info@talsmasoftware.nl">Sjoerd Talsma</a>
 */
public class AbstractDtoTest {

    // TODO separate into own java mapping tests.
//    ModelMapper domeinMapper;
    DtoRepresentationV1 valueObject;

    @Before
    public void setUp() {
//        domeinMapper = new ModelMapper();
//        domeinMapper.getConfiguration().setFieldMatchingEnabled(true);

        valueObject = new DtoRepresentationV1();
        valueObject.number = 42L;
        valueObject.name = "Representation name";
        valueObject.amountInEuros = new BigDecimal("12.50");
        valueObject.subObject = new DtoRepresentationV1();
        valueObject.subObject.number = 12L;
    }

    @Test
    public void javaSerialization() throws Exception {
        DtoRepresentationV1 copy = deserialize(serialize(valueObject));
        assertThat("copy", copy, is(not(sameInstance(valueObject))));
        assertThat("copy", copy, is(equalTo(valueObject)));
        assertThat("copy.number", copy.number, is(equalTo(42L)));
        assertThat("copy.name", copy.name, is(equalTo("Representation name")));
        assertThat("copy.subObject", copy.subObject, is(not(sameInstance(valueObject.subObject))));
        assertThat("copy.subObject", copy.subObject, is(equalTo(valueObject.subObject)));
        assertThat("copy.subObject.number", copy.subObject.number, is(equalTo(12L)));
        assertThat("copy.subObject.name", copy.subObject.name, is(nullValue()));
        assertThat("copy.subObject.subObject", copy.subObject.subObject, is(nullValue()));
    }

    @Test
    public void testEquals_null() {
        assertThat(valueObject, is(not(equalTo(null))));
    }

    @Test
    public void testEquals() {
        assertThat(valueObject, is(equalTo(valueObject)));
        DtoRepresentationV1 copy = new DtoRepresentationV1();
        copy.number = 42L;
        copy.name = "Representation name";
        copy.amountInEuros = new BigDecimal("12.50");
        copy.subObject = new DtoRepresentationV1();
        copy.subObject.number = 12L;
        assertThat(copy, is(equalTo(valueObject)));

        // Test BigDecimal equivalence.
        copy.amountInEuros = new BigDecimal("12.5");
        assertThat(copy.amountInEuros, is(not(equalTo(valueObject.amountInEuros)))); // standard !equals
        assertThat(copy, is(equalTo(valueObject))); // value object is 'functionally' equivalent.

        // Test for number
        copy.number = 40L;
        assertThat(copy, is(not(equalTo(valueObject))));

        // Test for name
        copy.number = 42L;
        assertThat(copy, is(equalTo(valueObject)));
        copy.name = "Kopie name";
        assertThat(copy, is(not(equalTo(valueObject))));

        // Test for subObject
        copy.name = "Representation name";
        assertThat(copy, is(equalTo(valueObject)));
        copy.subObject = null;
        assertThat(copy, is(not(equalTo(valueObject))));
        copy.subObject = new DtoRepresentationV1();
        assertThat(copy, is(not(equalTo(valueObject))));
        copy.subObject.number = 12L;
        copy.subObject.name = "Sub-object name";
        assertThat(copy, is(not(equalTo(valueObject))));
        copy.subObject.name = null;
        assertThat(copy, is(equalTo(valueObject)));
    }

    @Test
    public void testHashcode() {
        final int hashcode = valueObject.hashCode();
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));

        // Test BigDecimal equivalence.
        assertThat(valueObject.amountInEuros.hashCode(), is(not(equalTo(new BigDecimal("12.5").hashCode()))));
        valueObject.amountInEuros = new BigDecimal("12.5");
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));

        // Test op number
        valueObject.number = 40L;
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));

        // Test op name
        valueObject.number = 42L;
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));
        valueObject.name = "Andere name";
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));

        // Test op subObject
        valueObject.name = "Representation name";
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));
        valueObject.subObject = null;
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));
        valueObject.subObject = new DtoRepresentationV1();
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));
        valueObject.subObject.number = 12L;
        valueObject.subObject.name = "Sub-object name";
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));
        valueObject.subObject.name = null;
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));
    }

    @Test
    public void testToString() {
        assertThat(valueObject, hasToString(equalTo("DtoRepresentationV1{number=42, name=\"Representation name\", " +
                "amountInEuros=12.50, subObject=DtoRepresentationV1{number=12}}")));
        valueObject.subObject.subObject = new DtoRepresentationV1();
        valueObject.subObject.subObject.number = 99L;
        assertThat(valueObject, hasToString(equalTo("DtoRepresentationV1{number=42, name=\"Representation name\", " +
                "amountInEuros=12.50, subObject=DtoRepresentationV1{number=12, " +
                "subObject=DtoRepresentationV1{number=99}}}")));
    }

    @Test
    public void testClone() {
        DtoRepresentationV1 copy = (DtoRepresentationV1) valueObject.clone();
        assertThat(copy, is(not(sameInstance(valueObject))));
        assertThat(copy, is(equalTo(valueObject)));
        assertThat(copy.subObject, is(not(sameInstance(valueObject.subObject))));
        assertThat(copy.subObject, is(equalTo(valueObject.subObject)));
    }

    @Test
    public void testCircularReference() {
        valueObject.subObject.subObject = valueObject; // circle!

        // Test for finite hashCode() operation.
        assertThat(valueObject.hashCode(), is(equalTo(valueObject.hashCode())));

        // Test for finite equals() operation.
        assertThat(valueObject, is(equalTo(valueObject)));

        // Test for finite clone() operation.
        DtoRepresentationV1 copy = (DtoRepresentationV1) valueObject.clone();
        assertThat(copy, is(not(sameInstance(valueObject))));
        assertThat(copy.subObject, is(not(sameInstance(valueObject.subObject))));

        assertThat(copy.hashCode(), is(equalTo(valueObject.hashCode())));
        assertThat(copy.subObject.hashCode(), is(equalTo(valueObject.subObject.hashCode())));

        assertThat(copy, is(equalTo(valueObject)));
        assertThat(copy.subObject, is(equalTo(valueObject.subObject)));

        // Test cloned circle?
        assertThat(copy, is(sameInstance(copy.subObject.subObject)));

        // Change something somewhere in the structure:
        copy.subObject.subObject.subObject.subObject.subObject.number = 3;
        assertThat(copy.hashCode(), is(equalTo(copy.hashCode())));
        assertThat(copy.hashCode(), is(not(equalTo(valueObject.hashCode()))));
        assertThat(copy, is(not(equalTo(valueObject))));
    }

    public static byte[] serialize(Serializable object) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new ObjectOutputStream(bytes).writeObject(object);
            return bytes.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalStateException("Exception serializing object: " + object, ioe);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(byte[] bytes) {
        try {
            return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (IOException ioe) {
            throw new IllegalStateException("Exception deserializing object: " + ioe.getMessage(), ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException("Required class not found while deserializing object: " + cnfe.getMessage(), cnfe);
        }
    }

//    public static class DomeinObject {
//        private final long _private_nummer;
//        private final String _private_naam;
//        private final DomeinObject _private_subObject;
//
//        private DomeinObject(long number, String name, DomeinObject subObject) {
//            this._private_nummer = number;
//            this._private_naam = name;
//            this._private_subObject = subObject;
//        }
//
//        public long getNummer() {
//            return _private_nummer;
//        }
//
//        public String getNaam() {
//            return _private_naam;
//        }
//
//        public DomeinObject getSubObject() {
//            return _private_subObject;
//        }
//
//        public static class Builder {
//            private long number;
//            private String name;
//            private Builder parent, child;
//
//            public Builder number(long number) {
//                this.number = number;
//                return this;
//            }
//
//            public Builder name(String name) {
//                this.name = name;
//                return this;
//            }
//
//            public Builder child() {
//                if (child == null) {
//                    child = new Builder();
//                    child.parent = this;
//                }
//                return child;
//            }
//
//            public Builder parent() {
//                return parent;
//            }
//
//            public DomeinObject build() {
//                return new DomeinObject(number, name, child == null ? null : child.build());
//            }
//        }
//    }
}
