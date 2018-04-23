/*
 * Copyright 2016-2018 Talsma ICT
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.isFinal;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

/**
 * Class to abstract interaction with reflected properties in.
 * These can be mapped on public fields or getter / setter methods (or both).
 *
 * @author Sjoerd Talsma
 */
final class ReflectedBeanProperty implements BeanProperty {
    private static final Logger LOGGER = Logger.getLogger(ReflectedBeanProperty.class.getName());

    private final String name;
    private final Method getter, setter;
    private final Field field;
    private final Class<?> type;
    private volatile Collection annotations;

    ReflectedBeanProperty(Method getter, Method setter, Field field) {
        this.getter = getter;
        this.setter = setter;
        this.field = field;

        // Validation whether getter/setter/field correspond to the same logical property with a unique name and type.
        String nm = field == null ? null : field.getName();
        Class<?> tp = field == null ? null : field.getType();
        if (getter != null) {
            nm = validateSameName(nm, propertyNameOf(getter));
            tp = getter.getReturnType();
            // Bug #17: Type assumptions for bean fields are too strong.
//            Class<?> getterTp = getter.getReturnType();
//            if (tp == null) tp = getterTp;
//            else if (!getterTp.isAssignableFrom(tp)) throw new IllegalArgumentException("Incompatible getter type.");
        }
        if (setter != null) {
            nm = validateSameName(nm, propertyNameOf(setter));
            Class<?> setterTp = setter.getParameterTypes()[0];
            if (tp == null) tp = setterTp;
//            else if (!tp.isAssignableFrom(setterTp)) throw new IllegalArgumentException("Incompatible setter type.");
        }
        if (nm == null || tp == null) {
            throw new IllegalStateException("Either a field or getter/setter method must be provided!");
        }
        this.name = nm;
        this.type = tp;
    }

    static ReflectedBeanProperty merge(ReflectedBeanProperty other, Method getter, Method setter, Field field) {
        return new ReflectedBeanProperty(
                getter == null ? (other == null ? null : other.getter) : getter,
                setter == null ? (other == null ? null : other.setter) : setter,
                field == null ? (other == null ? null : other.field) : field);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isReadable() {
        return getter != null || field != null;
    }

    public boolean isWriteable() {
        return setter != null || (field != null && !isFinal(field.getModifiers()));
    }

    public Object read(Object bean) {
        Object value = null;
        if (isReadable()) {
            try {

                value = getter != null ? getter.invoke(bean) : field.get(bean);

            } catch (InvocationTargetException ite) {
                throw ite.getCause() instanceof RuntimeException ? (RuntimeException) ite.getCause()
                        : new IllegalStateException(String.format(
                        "Exception reading %s from %s: %s", this, bean, ite.getMessage()), ite);
            } catch (IllegalAccessException iae) {
                LOGGER.log(Level.FINE, "Not allowed to read {0} from {1} because: {2}", new Object[]{this, bean, iae});
            } catch (RuntimeException rte) {
                LOGGER.log(Level.FINE, "Could not read {0} from {1} because: {2}", new Object[]{this, bean, rte});
            }
        } else {
            LOGGER.log(Level.FINEST, "{0} is not readable in object: {1}", new Object[]{this, bean});
        }
        return value;
    }

    public boolean write(Object bean, Object propertyValue) {
        boolean written = false;
        if (isWriteable()) {
            try {

                if (setter != null) setter.invoke(bean, propertyValue);
                else field.set(bean, propertyValue);
                written = true;

            } catch (InvocationTargetException ite) {
                throw ite.getCause() instanceof RuntimeException ? (RuntimeException) ite.getCause()
                        : new IllegalStateException(String.format(
                        "Exception writing %s value %s to bean %s: %s", this, propertyValue, bean, ite.getMessage()), ite);
            } catch (IllegalAccessException iae) {
                LOGGER.log(Level.FINE, "Not allowed to write {0} value {1} to bean {2} because: {3}",
                        new Object[]{this, propertyValue, bean, iae});
            } catch (RuntimeException rte) {
                LOGGER.log(Level.FINE, "Could not write {0} to bean {1} to {2} because: {3}",
                        new Object[]{this, bean, propertyValue, rte});
            }
        } else {
            LOGGER.log(Level.FINEST, "{0} is not writeable in object: {1}", new Object[]{this, bean});
        }
        return written;
    }

    /**
     * Find and return the annotations on this bean property (on accessible field and/or descriptor method(s)).
     *
     * @return The annotations on this reflected bean property.
     */
    public Collection<Annotation> annotations() {
        if (annotations == null) {
            Collection<Annotation> foundAnnotations = new LinkedHashSet<Annotation>();
            if (getter != null) foundAnnotations.addAll(asList(getter.getDeclaredAnnotations()));
            if (setter != null) foundAnnotations.addAll(asList(setter.getDeclaredAnnotations()));
            if (field != null) foundAnnotations.addAll(asList(field.getDeclaredAnnotations()));
            annotations = unmodifiableCopy(foundAnnotations);
        }
        return annotations;
    }

    @Override
    public String toString() {
        return getName() + "{readable=" + isReadable() + ", writeable=" + isWriteable() + '}';
    }

    private static <T> Collection<T> unmodifiableCopy(Collection<T> coll) {
        if (coll == null) return null;
        else if (coll.isEmpty()) return emptySet();
        else if (coll.size() == 1) return singleton(coll.iterator().next());
        final List<T> copy = new ArrayList<T>(coll.size());
        copy.addAll(coll);
        return unmodifiableList(copy);
    }

    /**
     * The property name of the specified method.
     *
     * @param method The method to interpret as getter or setter
     * @return The property name or {@code null} if the method is not a getter or setter.
     */
    static String propertyNameOf(Method method) {
        String name = null;
        int params = method.getParameterTypes().length;
        if (params == 0 && method.getName().startsWith("is") && isBoolean(method.getReturnType())) {
            char[] nm = method.getName().substring(2).toCharArray();
            if (nm.length > 0) {
                nm[0] = Character.toLowerCase(nm[0]);
                name = new String(nm);
            }
        } else if (params == 0 && method.getName().startsWith("get") && !isVoid(method.getReturnType())) {
            char[] nm = method.getName().substring(3).toCharArray();
            if (nm.length > 0) {
                nm[0] = Character.toLowerCase(nm[0]);
                name = new String(nm);
            }
        } else if (params == 1 && method.getName().startsWith("set") && isVoid(method.getReturnType())) {
            char[] nm = method.getName().substring(3).toCharArray();
            if (nm.length > 0) {
                nm[0] = Character.toLowerCase(nm[0]);
                name = new String(nm);
            }
        }
        return name;
    }

    private static String validateSameName(String current, String newName) {
        if (current == null) {
            if (newName == null) throw new IllegalArgumentException("Property name is <null>.");
        } else if (!current.equals(newName)) {
            throw new IllegalArgumentException("Property name difference: \"" + current + "\" vs. \"" + newName + "\".");
        }
        return newName;
    }

    private static boolean isBoolean(Class<?> type) {
        return boolean.class.equals(type) || Boolean.class.isAssignableFrom(type);
    }

    private static boolean isVoid(Class<?> type) {
        return void.class.equals(type) || Void.class.isAssignableFrom(type);
    }

}
