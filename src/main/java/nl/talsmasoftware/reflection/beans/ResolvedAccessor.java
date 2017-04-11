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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Class to encapsulate a concrete property combined with a bean containing it.
 *
 * @author Sjoerd Talsma
 */
final class ResolvedAccessor {
    private static final Logger LOGGER = Logger.getLogger(ResolvedAccessor.class.getName());
    private static final Pattern ALL_DIGITS = Pattern.compile("\\d+");

    private final Object bean;
    private final BeanProperty property;
    private final int iterableIndex;

    private ResolvedAccessor(Object bean, BeanProperty property, int iterableIndex) {
        if (bean == null) throw new NullPointerException(
                "Cannot instantiate reflected property reference for bean <null>.");
        this.bean = bean;
        this.property = property;
        this.iterableIndex = iterableIndex;
    }

    static ResolvedAccessor of(Object bean, String propertyName, BeanProperty property) {
        if (bean != null) {
            if (property != null) {
                return new ResolvedAccessor(bean, property, -1);
            } else if (isPositiveInt(propertyName) && isIterable(bean)) {
                return new ResolvedAccessor(bean, null, Integer.valueOf(propertyName));
            }
        }
        return null;
    }

    private static boolean isPositiveInt(String name) {
        return ALL_DIGITS.matcher(name).matches();
    }

    private static boolean isIterable(Object bean) {
        return bean.getClass().isArray() || bean instanceof Iterable;
    }

    Object read() {
        if (property != null) return property.read(bean);
        else if (bean.getClass().isArray()) return readArrayIndex(bean, iterableIndex);
        else if (bean instanceof Iterable) return readIterableIndex((Iterable) bean, iterableIndex);
        return null;
    }

    boolean write(final Object value) {
        if (property != null) return property.write(bean, value);
        else if (bean.getClass().isArray()) return writeArrayIndex(bean, iterableIndex, value);
        // TODO Write iterable? At least for Lists we can give it a try :)
        return false;
    }

    private static Object readArrayIndex(Object array, int index) {
        try {
            if (array instanceof Object[]) return ((Object[]) array)[index];
            else if (array instanceof long[]) return ((long[]) array)[index];
            else if (array instanceof int[]) return ((int[]) array)[index];
            else if (array instanceof short[]) return ((short[]) array)[index];
            else if (array instanceof double[]) return ((double[]) array)[index];
            else if (array instanceof float[]) return ((float[]) array)[index];
            else if (array instanceof boolean[]) return ((boolean[]) array)[index];
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            LOGGER.log(Level.FINEST, "Array does not contain element at index: {0}.", index);
        }
        return null;
    }

    private static boolean writeArrayIndex(Object array, int index, Object value) {
        try {
            if (array instanceof Object[]) ((Object[]) array)[index] = value;
            else if (array instanceof long[]) ((long[]) array)[index] = (Long) value;
            else if (array instanceof int[]) ((int[]) array)[index] = (Integer) value;
            else if (array instanceof short[]) ((short[]) array)[index] = (Short) value;
            else if (array instanceof double[]) ((double[]) array)[index] = (Double) value;
            else if (array instanceof float[]) ((float[]) array)[index] = (Float) value;
            else if (array instanceof boolean[]) ((boolean[]) array)[index] = (Boolean) value;
            return true;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            LOGGER.log(Level.FINEST, "Array does not contain element at index: {0}.", index);
        }
        return false;
    }

    private static Object readIterableIndex(Iterable<?> iterable, int index) {
        if (iterable instanceof List) try {
            return ((List<?>) iterable).get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            LOGGER.log(Level.FINEST, "List does not contain element at index: {0}.", index);
        }
        int cursor = 0;
        for (Object val : iterable) if (index == cursor++) return val;
        return null;
    }

}
