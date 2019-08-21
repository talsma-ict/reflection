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
package nl.talsmasoftware.reflection.dto;

import nl.talsmasoftware.reflection.strings.ToStringBuilder;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static nl.talsmasoftware.reflection.beans.BeanReflection.getPropertyValues;
import static nl.talsmasoftware.reflection.beans.BeanReflection.setPropertyValue;

/**
 * Abstract base class for DTO (data transfer objects) where {@link Object objects} are purely used for data
 * representation, without any relevant object functionality. Traditionally, people often use the JavaBeans convention
 * for such objects, meaning for every attribute, there will be a private field defined plus a getter and/or a setter
 * method. This is rather cumbersome to program for a class where you're only interested in the data and (considering
 * there are getters and setters) mutability is not a real issue. There are libraries such a Lombok to help you with
 * this by generating this code for you.
 * <p>
 * However, what is inherently wrong with plain public fields in this case? What do the getters and setters actually buy
 * you in 'percieved' encapsulation?
 * <p>
 * However, wouldn't it be nice to be able to use simply public fields and no methods whatsoever, but still be able to
 * use the objects in {@link Object#hashCode() hash-based} collections, call {@link Object#equals(Object) equals()}
 * and {@link Object#toString() toString()} methods or even {@link Object#clone() clone} the object?
 * <p>
 * Well, now you can. Just extend this AbstractDTO object, add public fields and you're ready with your data transfer
 * object.
 * <p>
 * This abstract base class explicitly <strong>does</strong> support the use of public fields. What if you need to
 * 'override' a field with a getter / setter method? No problem! This abstract superclass will detect property accessor
 * methods and use those when applicable over the public fields.
 *
 * @author Sjoerd Talsma
 */
public abstract class AbstractDto implements Serializable, Cloneable {
    // Recursion detection in circular data structures contained in property values:
    private enum RecursionDetectors {
        HASHCODE, EQUALS, CLONE;
        private final ThreadLocal<Map<Object, Object>> recursionDetector = new ThreadLocal<Map<Object, Object>>() {
            @Override
            protected Map<Object, Object> initialValue() {
                return new IdentityHashMap<Object, Object>();
            }
        };
    }

    /**
     * Default constructor.
     * <p>
     * This constructor was made protected to discourage trying to instantiate this <strong>abstract</strong> class
     * directly.
     */
    protected AbstractDto() {
    }

    /**
     * Implementation of {@link Object#equals(Object) equals} based on the public fields and property accessor methods
     * of the concrete subclass.
     * <p>
     * <em>BigDecimals:</em> Please note that {@link BigDecimal} property values will <strong>not</strong> be compared
     * by the standard <code>equals()</code> implementation of BigDecimal, as that will declare the same number with
     * different precision to be unequal (e.g. "0" is unequal to "0.00" by default). This implementation uses the
     * {@link BigDecimal#compareTo(BigDecimal) compareTo()} method of <code>BigDecimal</code> instead, yielding
     * mathematical "0" and "0.00" equal to each-other. Please note that although that is technically not correct, it
     * is very useful for e.g. monetary value comparisons etc.
     *
     * @param other The other object to compare with.
     * @return <code>true</code> if the other object is also an instance of the same concrete subclass and all
     * properties have the same value, otherwise <code>false</code>.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!getClass().isInstance(other)) {
            return false;
        }
        final Map<Object, Object> equivalentObjects = RecursionDetectors.EQUALS.recursionDetector.get();
        if (equivalentObjects.containsKey(this)) {
            return other == equivalentObjects.get(this); // Recursion: Intentionally don't call equals() here.
        } else try {
            equivalentObjects.put(this, other);
            return equals(getPropertyValues(this), getPropertyValues(other));
        } finally {
            equivalentObjects.remove(this);
        }
    }

    /**
     * Implementation of {@link Object#hashCode() hashCode} based on the public fields and property accessor methods
     * of the concrete subclass.
     * <p>
     * <em>BigDecimals:</em> Please note that this method contains a workaround for {@link BigDecimal} property values,
     * so their <code>hashCode</code> outcome will be compatible with the {@link #equals(Object)} outcome. This is
     * simply achieved by using the hash code of the {@link BigDecimal#doubleValue() double value} instead.
     *
     * @return A hashCode that is based on the hashes of all public fields and property accessor methods of the concrete
     * subclass.
     */
    @Override
    public int hashCode() {
        int result = 1;
        final Map<Object, Object> hashcodes = RecursionDetectors.HASHCODE.recursionDetector.get();
        if (hashcodes.containsKey(this)) { // Recursion: circular object structure; return fixed hashcode.
            return result;
        } else try {
            hashcodes.put(this, this);
            for (Object obj : getPropertyValues(this).values()) {
                result = result + 31 * hashCode(obj);
            }
            return result;
        } finally {
            hashcodes.remove(this);
        }
    }

    /**
     * Implementation of {@link Object#toString() toString} based on the public fields and property accessor methods
     * of the concrete subclass. This method is delegated to the dedicated {@link ToStringBuilder} helper class that
     * also supports reflective toString resolution.
     * <p>
     * The names and values of all public properties will be {@link ToStringBuilder#append(CharSequence, Object) added}
     * to the resulting String.
     *
     * @return A readable toString representation based on the standard <code>ToStringBuilder</code>.
     * @see ToStringBuilder
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflect(this).toString();
    }

    /**
     * Implementation of {@link Object#clone() clone} based on the public fields and property accessor methods of the
     * concrete subclass. All properties are 'deep' cloned if they are also {@link Cloneable} objects themselves.
     *
     * @return A cloned copy of this object.
     */
    @Override
    public AbstractDto clone() {
        final Map<Object, Object> clones = RecursionDetectors.CLONE.recursionDetector.get();
        if (clones.containsKey(this)) { // Recursion: Return object reference to already-cloned object.
            return (AbstractDto) clones.get(this);
        } else try {
            AbstractDto clone = (AbstractDto) super.clone();
            clones.put(this, clone);
            for (Map.Entry<String, Object> property : getPropertyValues(this).entrySet()) {
                if (!"class".equals(property.getKey())) { // The property "class" should obviously not be cloned.
                    setPropertyValue(clone, property.getKey(),
                            cloneIfCloneable(property.getKey(), property.getValue()));
                }
            }
            return clone;
        } catch (CloneNotSupportedException cnse) {
            throw new IllegalStateException(String.format("Clone is not supported for %s.", this), cnse);
        } finally {
            clones.remove(this);
        }
    }

    /**
     * Returns the result of <code>propertyValue.clone()</code> if it implements the {@link Cloneable} interface, or
     * the <code>propertyValue</code> itself if it doesn't.
     *
     * @param propertyName  The name of the property being cloned.
     * @param propertyValue The value of the property being cloned.
     * @return The (possibly cloned) property value.
     */
    private Object cloneIfCloneable(String propertyName, Object propertyValue) {
        try {

            return propertyValue instanceof Cloneable
                    ? propertyValue.getClass().getMethod("clone").invoke(propertyValue)
                    : propertyValue;

        } catch (NoSuchMethodException nsme) {
            throw new IllegalStateException(String.format(
                    "Clone method is not found for property \"%s\" of %s.", propertyName, this), nsme);
        } catch (IllegalAccessException iae) {
            throw new IllegalStateException(String.format(
                    "Not allowed to clone property \"%s\" of %s.", propertyName, this), iae);
        } catch (InvocationTargetException ite) {
            throw ite.getCause() instanceof RuntimeException ? (RuntimeException) ite.getCause()
                    : new IllegalStateException(String.format(
                    "Exception while cloning property \"%s\" of %s.", propertyName, this), ite.getCause());
        }
    }

    /**
     * Hashcode implementation that returns the hashCode for various objects, so they match the equals definition.
     *
     * @param obj The object to calculate the hash code for (might be <code>null</code> for properties without a value).
     * @return The calculated hash code.
     */
    protected int hashCode(Object obj) {
        return obj == null ? 0
                : obj instanceof BigDecimal ? Double.valueOf(((BigDecimal) obj).doubleValue()).hashCode()
                : obj.hashCode();
    }

    /**
     * Equals implementation that provides specific equality implementations for {@link Map}, {@link List},
     * {@code Array} and {@link BigDecimal} types.
     * <p>
     * The {@link Map} implementation was added to be able to simply determine equality of all reflected properties of
     * concrete DTO object instances (which are returned as a <code>Map</code> from {@link String} to {@link Object}).
     * <p>
     * {@link BigDecimal} values are evaluated based on their {@link BigDecimal#compareTo(BigDecimal)} results.
     * Example: BigDecimals {@code "0"} and {@code "0.00"} are mathematically /scientifically unequal due to different
     * precisions, but represent the same numerical value. Equals will therefore return {@code true} in this case.
     *
     * @param objectA The first object to be compared for equality
     *                (might be <code>null</code> for properties without a value).
     * @param objectB The second object to be compared for equality
     *                (might be <code>null</code> for properties without a value).
     * @return <code>true</code> if the two objects are equal to each-other.
     */
    protected boolean equals(Object objectA, Object objectB) {
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
        return objectA != null && objectA.equals(objectB); // == check above will handle the nulls
    }

    /**
     * Specific method for equality of {@link Map} objects to be able to compare to property maps.
     *
     * @param mapA The first map to be compared.
     * @param mapB The second map to be compared.
     * @return <code>true</code> if both maps contain the same amount of entries and <code>mapB</code> contains all
     * entries from <code>mapA</code> (based on the equality definition of this class).
     * @see #equals(Object, Object)
     */
    private boolean mapEquals(Map<?, ?> mapA, Map<?, ?> mapB) {
        if (mapA.size() != mapB.size()) return false;
        for (Map.Entry<?, ?> entryA : mapA.entrySet()) {
            if (!mapB.containsKey(entryA.getKey()) || !equals(entryA.getValue(), mapB.get(entryA.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Specific method for equality of {@link List} objects that uses the equality definition in this class for all
     * list items in-order.
     *
     * @param listA The first list to be compared.
     * @param listB The second list to be compared.
     * @return <code>true</code> if both elements contain the same amount of elements (in the same order) based on the
     * equality definition of this class.
     * @see #equals(Object, Object)
     */
    private boolean listEquals(List<?> listA, List<?> listB) {
        if (listA.size() != listB.size()) return false;
        for (Iterator<?> itA = listA.iterator(), itB = listB.iterator(); itA.hasNext() && itB.hasNext(); ) {
            if (!equals(itA.next(), itB.next())) {
                return false;
            }
        }
        return true;
    }

}
