package nl.talsmasoftware.reflection.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.skyscreamer.jsonassert.JSONAssert;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test representatie objecten.
 * <p>
 * Aangezien de representatie objecten puur 'kale' data structuren zijn, hoeft het 'gedrag' van deze objecten niet
 * uitgebreid getest te worden. De serialisatie-eigenschappen (met name van- en naar JSON) zijn daarentegen wel
 * van belang.
 *
 * @author <a href="mailto:info@talsmasoftware.nl">Sjoerd Talsma</a>
 */
public class BaseRepresentationTest {

    public static class RepresentatieV1 extends BaseRepresentation {
        public long nummer;
        public String naam;
        public BigDecimal bedragInEuros;
        public RepresentatieV1 subObject;
    }

    ObjectMapper jsonMapper;
    ModelMapper domeinMapper;
    RepresentatieV1 valueObject;

    @Before
    public void setUp() {
        jsonMapper = new ObjectMapper();
        domeinMapper = new ModelMapper();
        domeinMapper.getConfiguration().setFieldMatchingEnabled(true);

        valueObject = new RepresentatieV1();
        valueObject.nummer = 42L;
        valueObject.naam = "Representatie naam";
        valueObject.bedragInEuros = new BigDecimal("12.50");
        valueObject.subObject = new RepresentatieV1();
        valueObject.subObject.nummer = 12L;
    }

    @Test
    public void javaSerialization() throws Exception {
        RepresentatieV1 kopie = deserialize(serialize(valueObject));
        assertThat("kopie", kopie, is(not(sameInstance(valueObject))));
        assertThat("kopie", kopie, is(equalTo(valueObject)));
        assertThat("kopie._private_nummer", kopie.nummer, is(equalTo(42L)));
        assertThat("kopie.naam", kopie.naam, is(equalTo("Representatie naam")));
        assertThat("kopie.subObject", kopie.subObject, is(not(sameInstance(valueObject.subObject))));
        assertThat("kopie.subObject", kopie.subObject, is(equalTo(valueObject.subObject)));
        assertThat("kopie.subObject._private_nummer", kopie.subObject.nummer, is(equalTo(12L)));
        assertThat("kopie.subObject.naam", kopie.subObject.naam, is(nullValue()));
        assertThat("kopie.subObject.subObject", kopie.subObject.subObject, is(nullValue()));
    }

    @Test
    public void deserializeFromJson() throws Exception {
        RepresentatieV1 deserialized = jsonMapper.readValue(fixture("/fixtures/value-object-1.json"), RepresentatieV1.class);
        assertThat("Uit JSON gelezen voorbeeld", deserialized, is(equalTo(valueObject)));
    }

    @Test
    public void serializeToJson() throws Exception {
        final String expected = fixture("/fixtures/value-object-1.json");
        String serialized = jsonMapper.writeValueAsString(valueObject);
        JSONAssert.assertEquals(expected, serialized, false);
    }

    @Test
    public void javaModelMapping() throws Exception {
        RepresentatieV1 mapped = domeinMapper.map(
                new DomeinObject.Builder()
                        .nummer(42L)
                        .naam("Domein naam")
                        .child().nummer(12L).parent()
                        .build(),
                RepresentatieV1.class);

        assertThat(mapped, is(not(nullValue())));
        assertThat(mapped.nummer, is(equalTo(42L)));
        assertThat(mapped.naam, is(equalTo("Domein naam")));
        assertThat(mapped.subObject, is(not(nullValue())));
        assertThat(mapped.subObject.nummer, is(equalTo(12L)));
    }

    @Test
    public void testEquals_null() {
        assertThat(valueObject, is(not(equalTo(null))));
    }

    @Test
    public void testEquals() {
        assertThat(valueObject, is(equalTo(valueObject)));
        RepresentatieV1 kopie = new RepresentatieV1();
        kopie.nummer = 42L;
        kopie.naam = "Representatie naam";
        kopie.bedragInEuros = new BigDecimal("12.50");
        kopie.subObject = new RepresentatieV1();
        kopie.subObject.nummer = 12L;
        assertThat(kopie, is(equalTo(valueObject)));

        // Test BigDecimal equivalentie.
        kopie.bedragInEuros = new BigDecimal("12.5");
        assertThat(kopie.bedragInEuros, is(not(equalTo(valueObject.bedragInEuros)))); // standaard !equals
        assertThat(kopie, is(equalTo(valueObject))); // maar value object is wel 'functioneel' equivalent.

        // Test op nummer
        kopie.nummer = 40L;
        assertThat(kopie, is(not(equalTo(valueObject))));

        // Test op naam
        kopie.nummer = 42L;
        assertThat(kopie, is(equalTo(valueObject)));
        kopie.naam = "Kopie naam";
        assertThat(kopie, is(not(equalTo(valueObject))));

        // Test op subObject
        kopie.naam = "Representatie naam";
        assertThat(kopie, is(equalTo(valueObject)));
        kopie.subObject = null;
        assertThat(kopie, is(not(equalTo(valueObject))));
        kopie.subObject = new RepresentatieV1();
        assertThat(kopie, is(not(equalTo(valueObject))));
        kopie.subObject.nummer = 12L;
        kopie.subObject.naam = "Sub-object naam";
        assertThat(kopie, is(not(equalTo(valueObject))));
        kopie.subObject.naam = null;
        assertThat(kopie, is(equalTo(valueObject)));
    }

    @Test
    public void testHashcode() {
        final int hashcode = valueObject.hashCode();
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));

        // Test BigDecimal equivalentie.
        assertThat(valueObject.bedragInEuros.hashCode(), is(not(equalTo(new BigDecimal("12.5").hashCode()))));
        valueObject.bedragInEuros = new BigDecimal("12.5");
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));

        // Test op nummer
        valueObject.nummer = 40L;
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));

        // Test op naam
        valueObject.nummer = 42L;
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));
        valueObject.naam = "Andere naam";
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));

        // Test op subObject
        valueObject.naam = "Representatie naam";
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));
        valueObject.subObject = null;
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));
        valueObject.subObject = new RepresentatieV1();
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));
        valueObject.subObject.nummer = 12L;
        valueObject.subObject.naam = "Sub-object naam";
        assertThat(valueObject.hashCode(), is(not(equalTo(hashcode))));
        valueObject.subObject.naam = null;
        assertThat(valueObject.hashCode(), is(equalTo(hashcode)));
    }

    @Test
    public void testToString() {
        assertThat(valueObject, hasToString(equalTo("RepresentatieV1{nummer=42, naam=\"Representatie naam\", " +
                "bedragInEuros=12.50, subObject=RepresentatieV1{nummer=12}}")));
        valueObject.subObject.subObject = new RepresentatieV1();
        valueObject.subObject.subObject.nummer = 99L;
        assertThat(valueObject, hasToString(equalTo("RepresentatieV1{nummer=42, naam=\"Representatie naam\", " +
                "bedragInEuros=12.50, subObject=RepresentatieV1{nummer=12, " +
                "subObject=RepresentatieV1{nummer=99}}}")));
    }

    @Test
    public void testClone() {
        RepresentatieV1 kopie = (RepresentatieV1) valueObject.clone();
        assertThat(kopie, is(not(sameInstance(valueObject))));
        assertThat(kopie, is(equalTo(valueObject)));
        assertThat(kopie.subObject, is(not(sameInstance(valueObject.subObject))));
        assertThat(kopie.subObject, is(equalTo(valueObject.subObject)));
    }

    @Test
    public void testCirculaireReferentie() {
        valueObject.subObject.subObject = valueObject; // cirkel!

        // Test eindige hashCode() operatie.
        assertThat(valueObject.hashCode(), is(equalTo(valueObject.hashCode())));

        // Test eindige equals() operatie.
        assertThat(valueObject, is(equalTo(valueObject)));

        // Test eindige clone() operatie.
        RepresentatieV1 kopie = (RepresentatieV1) valueObject.clone();
        assertThat(kopie, is(not(sameInstance(valueObject))));
        assertThat(kopie.subObject, is(not(sameInstance(valueObject.subObject))));

        assertThat(kopie.hashCode(), is(equalTo(valueObject.hashCode())));
        assertThat(kopie.subObject.hashCode(), is(equalTo(valueObject.subObject.hashCode())));

        assertThat(kopie, is(equalTo(valueObject)));
        assertThat(kopie.subObject, is(equalTo(valueObject.subObject)));

        // Ergens in de structuur iets wijzigen:
        kopie.subObject.subObject.subObject.subObject.subObject.nummer = 3;
        assertThat(kopie.hashCode(), is(equalTo(kopie.hashCode())));
        assertThat(kopie.hashCode(), is(not(equalTo(valueObject.hashCode()))));
        assertThat(kopie, is(not(equalTo(valueObject))));
    }

    public static class DomeinObject {
        private final long _private_nummer;
        private final String _private_naam;
        private final DomeinObject _private_subObject;

        private DomeinObject(long nummer, String naam, DomeinObject subObject) {
            this._private_nummer = nummer;
            this._private_naam = naam;
            this._private_subObject = subObject;
        }

        public long getNummer() {
            return _private_nummer;
        }

        public String getNaam() {
            return _private_naam;
        }

        public DomeinObject getSubObject() {
            return _private_subObject;
        }

        public static class Builder {
            private long nummer;
            private String naam;
            private Builder parent, child;

            public Builder nummer(long nummer) {
                this.nummer = nummer;
                return this;
            }

            public Builder naam(String naam) {
                this.naam = naam;
                return this;
            }

            public Builder child() {
                if (child == null) {
                    child = new Builder();
                    child.parent = this;
                }
                return child;
            }

            public Builder parent() {
                return parent;
            }

            public DomeinObject build() {
                return new DomeinObject(nummer, naam, child == null ? null : child.build());
            }
        }
    }
}
