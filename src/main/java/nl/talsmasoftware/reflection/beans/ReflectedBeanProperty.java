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

import java.beans.PropertyDescriptor;
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

    private final PropertyDescriptor descriptor;
    private final Field field;
    private volatile Collection annotations;

    ReflectedBeanProperty(PropertyDescriptor descriptor, Field field) {
        if (descriptor == null && field == null) {
            throw new IllegalStateException("Either property descriptor or field must be provided!");
        }
        this.descriptor = descriptor;
        this.field = field;
    }

    ReflectedBeanProperty withDescriptor(PropertyDescriptor descriptor) {
        return new ReflectedBeanProperty(descriptor, field);
    }

    public String getName() {
        return descriptor != null ? descriptor.getName() : field.getName();
    }

    public Class<?> getType() {
        return descriptor != null ? descriptor.getPropertyType() : field.getType();
    }

    public boolean isReadable() {
        return (descriptor != null && descriptor.getReadMethod() != null)
                || field != null;
    }

    public boolean isWriteable() {
        return (descriptor != null && descriptor.getWriteMethod() != null)
                || (field != null && !isFinal(field.getModifiers()));
    }

    public Object read(Object bean) {
        Object value = null;
        if (isReadable()) {
            try {
                final Method readMethod = descriptor != null ? descriptor.getReadMethod() : null;
                value = readMethod != null ? readMethod.invoke(bean) : field.get(bean);
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
                final Method writeMethod = descriptor != null ? descriptor.getWriteMethod() : null;
                if (writeMethod != null) {
                    writeMethod.invoke(bean, propertyValue);
                    written = true;
                } else {
                    field.set(bean, propertyValue);
                    written = true;
                }
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
            if (descriptor != null) {
                if (descriptor.getReadMethod() != null)
                    foundAnnotations.addAll(asList(descriptor.getReadMethod().getDeclaredAnnotations()));
                if (descriptor.getWriteMethod() != null)
                    foundAnnotations.addAll(asList(descriptor.getWriteMethod().getDeclaredAnnotations()));
            }
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
}
