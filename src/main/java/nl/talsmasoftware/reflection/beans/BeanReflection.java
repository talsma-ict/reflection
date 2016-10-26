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
package nl.talsmasoftware.reflection.beans;

import nl.talsmasoftware.reflection.Classes;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.*;

/**
 * Support-class for bean reflection based on getter / setter methods and / or public field access for Java objects.
 * Unfortunately this is necessary because the standard {@link Introspector bean introspector} does not return any
 * public fields.
 * <p>
 * Class diagram:<br><center><img src="BeanReflectionSupport.svg" alt="class diagram"></center>
 *
 * @author Sjoerd Talsma
 */
public final class BeanReflection {
    private static final Logger LOGGER = Logger.getLogger(BeanReflection.class.getName());

    /**
     * Cache, similar to the Introspector method cache.
     */
    private static final Map<Class<?>, Reference<Map<String, ReflectedBeanProperty>>> reflectedPropertiesCache =
            new WeakHashMap<Class<?>, Reference<Map<String, ReflectedBeanProperty>>>();

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private BeanReflection() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method reflects the requested object and returns a map from property name to property instance.
     * The map may not be modified again after it has been returned from this method; therefore an unmodifiable view of
     * the map will be returned.
     *
     * @param source The object to be reflected.
     * @return The unmodifiable map of reflected properties for the specified object.
     */
    private static Map<String, ReflectedBeanProperty> reflectedPropertiesOf(Object source) {
        if (source == null) return emptyMap();
        final Class<?> sourceType = source instanceof Class ? (Class) source : source.getClass();
        Map<String, ReflectedBeanProperty> properties = null;
        synchronized (reflectedPropertiesCache) {
            Reference<Map<String, ReflectedBeanProperty>> reference = reflectedPropertiesCache.get(sourceType);
            if (reference != null) properties = reference.get();
        }
        if (properties == null) {
            properties = reflectProperties(sourceType);
            synchronized (reflectedPropertiesCache) {
                reflectedPropertiesCache.put(sourceType,
                        new WeakReference<Map<String, ReflectedBeanProperty>>(properties));
            }
        }
        return properties;
    }

    /**
     * The bean reflection logic, not to be called directly but always through {@link #reflectedPropertiesOf(Object)}.
     * That method provides important caching.
     *
     * @param sourceType The type to be reflected.
     * @return The map with reflected properties for the specified type.
     */
    private static Map<String, ReflectedBeanProperty> reflectProperties(Class<?> sourceType) {
        Map<String, ReflectedBeanProperty> properties = new LinkedHashMap<String, ReflectedBeanProperty>();
        if (sourceType != null) {
            addPublicFields(sourceType, properties);
            addPropertyDescriptors(sourceType, properties);
        }
        return unmodifiable(properties);
    }

    /**
     * Add all public fields for the <code>sourceType</code> to the map of reflected properties.
     *
     * @param sourceType The source type to be reflected.
     * @param properties The map of reflected properties to add public fields to.
     */
    private static void addPublicFields(Class<?> sourceType, Map<String, ReflectedBeanProperty> properties) {
        for (Field field : sourceType.getFields()) {
            if (isPublic(field.getModifiers()) && !isStatic(field.getModifiers())) {
                properties.put(field.getName(), new ReflectedBeanProperty(null, field));
            }
        }
    }

    /**
     * Add all {@link PropertyDescriptor property descriptors} from the {@link Introspector} to the map of reflected
     * properties.
     *
     * @param sourceType The source type to be reflected.
     * @param properties The map of reflected properties to add public fields to.
     */
    private static void addPropertyDescriptors(Class<?> sourceType, Map<String, ReflectedBeanProperty> properties) {
        try { // java.beans.Introspector properties:
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(sourceType).getPropertyDescriptors()) {
                String name = descriptor.getName();
                ReflectedBeanProperty property = properties.get(name);
                properties.put(name, property == null ? new ReflectedBeanProperty(descriptor, null)
                        : property.withDescriptor(descriptor));
            }
        } catch (IntrospectionException is) {
            LOGGER.log(Level.FINEST, "Could not reflect bean information of {0} because: {1}",
                    new Object[]{sourceType.getName(), is});
        } catch (RuntimeException beanException) {
            LOGGER.log(Level.FINEST, "Exception reflecting bean information of {0}: {1}",
                    new Object[]{sourceType.getName(), beanException});
        }
    }

    /**
     * This method flushes the caches of internally reflected information and asks the Bean {@link Introspector} to do
     * the same.
     */
    public static void flushCaches() {
        synchronized (reflectedPropertiesCache) {
            reflectedPropertiesCache.clear();
        }
        Introspector.flushCaches();
    }

    /**
     * This method returns a property value for a specific object instance.
     *
     * @param bean         The object instance to return the requested property value for.
     * @param propertyName The name of the requested bean property.
     * @return The value of the property or <code>null</code> in case it could not be reflected.
     */
    public static Object getPropertyValue(Object bean, String propertyName) {
        Object propertyValue = null;
        final ReflectedBeanProperty reflectedProperty = reflectedPropertiesOf(bean).get(propertyName);
        if (reflectedProperty == null) {
            LOGGER.log(Level.FINEST, "Property \"{0}\" not found in object: {1}", new Object[]{propertyName, bean});
        } else {
            propertyValue = reflectedProperty.read(bean);
        }
        return propertyValue;
    }

    /**
     * This method writes the specified property value in an object instance.
     * <p>
     * Obviously, the object should have a 'writable' property with the specified name.
     *
     * @param bean          The object instance to set the property value for.
     * @param propertyName  The name of the (writable) bean property to be set.
     * @param propertyValue The new property value to be set.
     * @return <code>true</code> if the property was succesfully set,
     * or <code>false</code> if this was not possibile for some reason.
     */
    public static boolean setPropertyValue(Object bean, String propertyName, Object propertyValue) {
        boolean result = false;
        final ReflectedBeanProperty reflectedProperty = reflectedPropertiesOf(bean).get(propertyName);
        if (reflectedProperty == null) {
            LOGGER.log(Level.FINEST, "Property \"{0}\" not found in object: {1}", new Object[]{propertyName, bean});
        } else {
            result = reflectedProperty.write(bean, propertyValue);
        }
        return result;
    }

    /**
     * This method returns the reflected properties for the specified bean.
     *
     * @param bean The object instance to reflect the bean properties of.
     * @return The reflected bean properties.
     */
    @SuppressWarnings("unchecked") // collection is read-only so this is safe.
    public static Collection<BeanProperty> getBeanProperties(Object bean) {
        final Collection<? extends BeanProperty> properties = reflectedPropertiesOf(bean).values();
        return (Collection<BeanProperty>) properties;
    }

    /**
     * This method returns all bean properties for the specified bean.
     *
     * @param bean The object instance to read the bean properties of.
     * @return A map of all bean property names mapping to the corresponding property values.
     */
    public static Map<String, Object> getPropertyValues(Object bean) {
        Map<String, Object> propertyValues = new LinkedHashMap<String, Object>();
        if (bean != null) for (BeanProperty property : getBeanProperties(bean)) {
            if (property.isReadable()) propertyValues.put(property.getName(), property.read(bean));
        }
        return unmodifiable(propertyValues);
    }

    /**
     * This method creates a new bean instance of the requested type using the default constructor and attempts to
     * set all the specified property values.
     *
     * @param beanType       The bean type to be instantiated using the default constructor.
     * @param propertyValues The properties that have to be set.
     * @param <T>            The bean type to be returned.
     * @return The instantiated bean with the specified property values.
     */
    public static <T> T createBean(Class<T> beanType, Map<String, Object> propertyValues) {
        if (beanType == null) throw new IllegalArgumentException("Bean type was null.");
        T bean = Classes.createNew(beanType);
        if (propertyValues != null) for (Map.Entry<String, Object> propertyValue : propertyValues.entrySet()) {
            setPropertyValue(bean, propertyValue.getKey(), propertyValue.getValue());
        }
        return bean;
    }

    /**
     * Returns an unmodifiable view of the given map.
     *
     * @param map The map to be made unmodifiable.
     * @param <K> The type of the map keys.
     * @param <V> The type of the map values.
     * @return An unmodifiable view of the given map.
     */
    private static <K, V> Map<K, V> unmodifiable(Map<K, V> map) {
        switch (map.size()) {
            case 0:
                return emptyMap();
            case 1:
                final Map.Entry<K, V> entry = map.entrySet().iterator().next();
                return singletonMap(entry.getKey(), entry.getValue());
            default:
                return unmodifiableMap(map);
        }
    }

}
