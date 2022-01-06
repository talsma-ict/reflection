/*
 * Copyright 2016-2022 Talsma ICT
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

import nl.talsmasoftware.reflection.Classes;
import nl.talsmasoftware.reflection.Methods;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.*;
import static nl.talsmasoftware.reflection.beans.ReflectedBeanProperty.propertyNameOf;

/**
 * Support-class for bean reflection based on getter / setter methods and / or public field access for Java objects.
 * Unfortunately this is necessary because the standard Java bean introspector}does not return any  public fields.
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
            for (Field field : sourceType.getFields()) {
                if (isPublic(field.getModifiers()) && !isStatic(field.getModifiers())) {
                    properties.put(field.getName(), new ReflectedBeanProperty(null, null, field));
                }
            }
            for (Method method : methodsOf(sourceType, new LinkedHashSet<Method>())) {
                String propertyName = propertyNameOf(method);
                if (propertyName != null) {
                    Method getter = null, setter = null;
                    if (method.getParameterTypes().length == 0) getter = method;
                    else setter = method;
                    ReflectedBeanProperty current = properties.get(propertyName);
                    properties.put(propertyName, ReflectedBeanProperty.merge(current, getter, setter, null));
                }
            }
        }
        return unmodifiable(properties);
    }

    private static Collection<Method> methodsOf(Class<?> type, Collection<Method> acc) {
        if (type != null) {
            for (Method m : Methods.getDeclaredMethods(type)) {
                if (isPublic(m.getModifiers()) && !isStatic(m.getModifiers())) acc.add(m);
            }
            return methodsOf(type.getSuperclass(), acc);
        }
        return acc;
    }

    /**
     * This method flushes the caches of internally reflected information.
     */
    public static void flushCaches() {
        synchronized (reflectedPropertiesCache) {
            reflectedPropertiesCache.clear();
        }
    }

    /**
     * Looks for a property (possibly recursive) and returns the reference containing the containing bean and the
     * property. This is necessary because the containing bean can be a different one from the specified bean (due to
     * one or more inner properties separated by dots).
     *
     * @param bean         The outer bean to obtain the property reference for.
     * @param propertyName The name of the property to obtain from the bean.
     * @return The propertey reference that can be accessed or <code>null</code> if not found.
     */
    private static ResolvedAccessor findPropertyAccessor(final Object bean, final String propertyName) {
        ResolvedAccessor accessor = null;
        if (bean != null && propertyName != null) {
            Object src = bean;
            String propName = propertyName.replaceAll("\\[([^]]*)]", ".$1"); // Replace array notation with dot.
            final int dotIdx = propName.lastIndexOf('.');
            if (dotIdx >= 0) {
                src = getPropertyValue(bean, propName.substring(0, dotIdx));
                propName = propName.substring(dotIdx + 1);
            }
            accessor = ResolvedAccessor.of(src, propName, reflectedPropertiesOf(src).get(propName));
        }
        if (accessor == null) LOGGER.log(Level.FINEST, "Property \"{0}\" not found in object: {1}",
                new Object[]{propertyName, bean});
        return accessor;
    }

    /**
     * This method returns a property value for a specific object instance.
     *
     * @param bean         The object instance to return the requested property value for.
     * @param propertyName The name of the requested bean property.
     * @return The value of the property or <code>null</code> in case it could not be reflected.
     */
    public static Object getPropertyValue(final Object bean, final String propertyName) {
        final ResolvedAccessor accessor = findPropertyAccessor(bean, propertyName);
        return accessor != null ? accessor.read() : null;
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
        final ResolvedAccessor accessor = findPropertyAccessor(bean, propertyName);
        return accessor != null && accessor.write(propertyValue);
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
