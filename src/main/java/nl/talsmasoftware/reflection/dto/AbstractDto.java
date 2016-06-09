package nl.talsmasoftware.reflection.dto;

import nl.talsmasoftware.reflection.beans.BeanReflectionSupport;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

/**
 * Abstracte basisklasse voor representatie objecten waarbij {@link Object#equals(Object)}, {@link Object#hashCode()},
 * {@link Object#toString()} en <code>clone()</code> zijn geimplementeerd op basis van de publieke properties
 * van het object.
 * <p>
 * Het is expliciet <em>wel</em> de bedoeling om hierbij publieke velden ook te ondersteunen
 * (naast javabean getters en setters)!
 *
 * @author <a href="mailto:info@talsmasoftware.nl">Sjoerd Talsma</a>
 */
public abstract class AbstractDto implements Serializable, Cloneable {
    // Recursie detectie in circulaire property waarden:
    private enum RecurionDetectors {
        HASHCODE, EQUALS, CLONE;
        private final ThreadLocal<Map<Object, Object>> recursionDetector = new ThreadLocal<Map<Object, Object>>() {
            @Override
            protected Map<Object, Object> initialValue() {
                return new IdentityHashMapMap<Object, Object>();
            }
        };
    }

    /**
     * Default constructor.
     * <p>
     * Deze is protected gemaakt om het nog iets moeilijker te maken om rechtstreeks een instantie van deze abstracte
     * klasse proberen te instanti&euml;ren.
     */
    protected AbstractDto() {
    }

    /**
     * Implementatie van equals op basis van de publieke velden van de concrete subklasse.
     *
     * @param other Het andere object om mee te vergelijken.
     * @return {@code true} indien het andere object ook een instantie is van de concrete subklasse en alle publieke
     * velden overeenkomende waarden hebben, anders {@code false}.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!getClass().isInstance(other)) {
            return false;
        }
        Map<Object, Object> equivalentObjects = RecurionDetectors.EQUALS.recursionDetector.get();
        if (equivalentObjects.containsKey(this)) {
            return other == equivalentObjects.get(this); // Recursie: Hier bewust geen equals() meer aanroepen.
        } else try {
            equivalentObjects.put(this, other);
            return equals(getPropertyValues(this), getPropertyValues(other));
        } finally {
            equivalentObjects.remove(this);
        }
    }

    /**
     * Implementatie van hashCode op basis van de publieke velden van de concrete subklasse.
     * <p>
     * Hierin zit een workaround ingebakken voor {@link BigDecimal} objecten; de hashcode berekening moet daarvan
     * in lijn zijn met de equals berekening; daarom wordt daarvoor de
     * {@link BigDecimal#doubleValue() double waarde} gebruikt.
     *
     * @return Een hashCode waarbij alle publieke velden van de concrete subklasse in meegenomen zijn.
     */
    @Override
    public int hashCode() {
        int result = 1;
        Map<Object, Object> hashcodes = RecurionDetectors.HASHCODE.recursionDetector.get();
        if (hashcodes.containsKey(this)) { // Recursie: circulaire object structuur; geef vaste hashcode terug.
            return result;
        } else try {
            hashcodes.put(this, this);
            // [ST] dit werkt ALLEEN als we de properties in dezelfde volgorde krijgen: ?? Garantie voor inbouwen ??
            for (Object obj : getPropertyValues(this).values()) {
                result = 31 * result + Objects.hashCode(obj instanceof BigDecimal ? ((BigDecimal) obj).doubleValue() : obj);
            }
            return result;
        } finally {
            hashcodes.remove(this);
        }
    }

    /**
     * Implementatie van toString op basis van de standaard ingestelde {@link ToStringBuilder},
     * waarbij de namen + waarden van alle publieke velden van de concrete subklasse
     * worden {@link ToStringBuilder#append(CharSequence, Object) toegevoegd}.
     *
     * @return De toString representatie op basis van de standaard {@code ToStringBuilder}.
     * @see ToStringBuilder
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflect(this).toString();
    }

    /**
     * Implementatie van clone waarbij de waarden van alle publieke velden 'diep' worden gecloned indien deze ook
     * {@link Cloneable} zijn.
     *
     * @return Een kopie van dit object.
     */
    @Override
    public AbstractDto clone() {
        Map<Object, Object> clones = RecurionDetectors.CLONE.recursionDetector.get();
        if (clones.containsKey(this)) { // Recursie: Geeft referentie naar al gecloned object terug.
            return (AbstractDto) clones.get(this);
        } else try {
            AbstractDto clone = (AbstractDto) super.clone();
            clones.put(this, clone);
            for (Map.Entry<String, Object> property : BeanReflectionSupport.getPropertyValues(this).entrySet()) {
                if (!"class".equals(property.getKey())) { // De property "class" kan uiteraard niet gekopieerd worden.
                    Object value = property.getValue();
                    if (value instanceof Cloneable) {
                        value = value.getClass().getMethod("clone").invoke(value);
                    }
                    BeanReflectionSupport.setPropertyValue(clone, property.getKey(), value);
                }
            }
            return clone;
        } catch (CloneNotSupportedException | ReflectiveOperationException cloneFout) {
            throw new IllegalStateException(String.format("Clone is niet mogelijk voor %s.", this), cloneFout);
        } finally {
            clones.remove(this);
        }
    }

    /**
     * Equals implementatie die specifieke vergelijking voor {@link Map}, {@link List}, {@code Array} vergelijking en
     * BigDecimal bevat.
     * <p>
     * De {@link Map} implementatie is toegevoegd om eenvoudig alle gereflecteerde properties op inhoud te kunnen
     * vergelijken.
     * <p>
     * De {@link List} en {@code Object[]} implementatie is toegevoegd om deze specifieke equals voor objecten in een
     * vaste volgorde te kunnen gebruiken.
     * <p>
     * Voor {@link BigDecimal} waarden die mathematisch niet gelijk zijn, maar qua daadwerkelijke waarde wel, wordt
     * de 'waarde' vergelijking gehanteerd.
     * Voorbeeld: BigDecimals {@code "0"} en {@code "0.00"} zijn mathematisch ongelijk vanwege ongelijke
     * nauwkeurigheid, maar representeren beide dezelfde waarde, equals zal in dit geval dan ook {@code true} opleveren.
     */
    private static boolean equals(Object objectA, Object objectB) {
        if (objectA == objectB) {
            return true;
        } else if (objectA instanceof Map && objectB instanceof Map) {
            return mapEquals((Map<?, ?>) objectA, (Map<?, ?>) objectB);
        } else if (objectA instanceof List && objectB instanceof List) {
            return listEquals((List<?>) objectA, (List<?>) objectB);
        } else if (objectA instanceof Object[] && objectB instanceof Object[]) {
            return listEquals(asList((Object[]) objectA), asList((Object[]) objectB));
        } else if (objectA instanceof BigDecimal && objectB instanceof BigDecimal) {
            return ((BigDecimal) objectA).compareTo((BigDecimal) objectB) == 0;
        }
        return Objects.equals(objectA, objectB);
    }

    /**
     * Map specifieke equality methode om de properties van twee objecten te kunnen vergelijken waarbij waarden weer
     * recursief via de algemene {@link #equals(Object, Object)} worden gedelegeerd.
     *
     * @param mapA De ene map om te vergelijken.
     * @param mapB De andere map om te vergelijken.
     * @return {@code true} indien beide maps evenveel entries bevatten en map B alle entries van map A ook bevat
     * (op basis van de equals definitie in deze klasse).
     * @see #equals(Object, Object)
     */
    private static boolean mapEquals(Map<?, ?> mapA, Map<?, ?> mapB) {
        if (mapA.size() != mapB.size()) {
            return false;
        }
        for (Map.Entry<?, ?> entryA : mapA.entrySet()) {
            if (!mapB.containsKey(entryA.getKey()) || !equals(entryA.getValue(), mapB.get(entryA.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * List specifieke equality methode waarbij waarden weer recursief via de algemene {@link #equals(Object, Object)}
     * worden gedelegeerd.
     *
     * @param listA De ene lijst om te vergelijken.
     * @param listB De andere lijst om te vergelijken.
     * @return {@code true} indien beide lijsten evenveel elementen bevatten en alle elementen (in dezelfde volgorde)
     * gelijk zijn op basis van de equals definitie in deze klasse.
     * @see #equals(Object, Object)
     */
    private static boolean listEquals(List<?> listA, List<?> listB) {
        if (listA.size() != listB.size()) {
            return false;
        }
        for (Iterator<?> itA = listA.iterator(), itB = listB.iterator(); itA.hasNext() && itB.hasNext(); ) {
            if (!equals(itA.next(), itB.next())) {
                return false;
            }
        }
        return true;
    }

}
