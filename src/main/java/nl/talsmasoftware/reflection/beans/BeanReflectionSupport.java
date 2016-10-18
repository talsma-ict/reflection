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

import java.beans.Introspector;
import java.util.Collection;
import java.util.Map;

/**
 * Support-class for bean reflection based on getter / setter methods and / or public field access for Java objects.
 * Unfortunately this is necessary because the standard {@link Introspector bean introspector} does not return any
 * public fields.
 * <p>
 * Class diagram:<br><center><img src="BeanReflectionSupport.svg" alt="class diagram"></center>
 *
 * @author Sjoerd Talsma
 * @deprecated This utility class was renamed to <code>BeanReflection</code>.
 */
public final class BeanReflectionSupport {

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private BeanReflectionSupport() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method flushes the caches of internally reflected information and asks the Bean {@link Introspector} to do
     * the same.
     *
     * @see BeanReflection#flushCaches()
     */
    public static void flushCaches() {
        BeanReflection.flushCaches();
    }

    /**
     * This method returns a property value for a specific object instance.
     *
     * @param bean         The object instance to return the requested property value for.
     * @param propertyName The name of the requested bean property.
     * @return The value of the property or <code>null</code> in case it could not be reflected.
     * @see BeanReflection#getPropertyValue(Object, String)
     */
    public static Object getPropertyValue(Object bean, String propertyName) {
        return BeanReflection.getPropertyValue(bean, propertyName);
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
     * @see BeanReflection#setPropertyValue(Object, String, Object)
     */
    public static boolean setPropertyValue(Object bean, String propertyName, Object propertyValue) {
        return BeanReflection.setPropertyValue(bean, propertyName, propertyValue);
    }

    /**
     * This method returns the reflected properties for the specified bean.
     *
     * @param bean The object instance to reflect the bean properties of.
     * @return The reflected bean properties.
     * @see BeanReflection#getBeanProperties(Object)
     */
    public static Collection<BeanProperty> getBeanProperties(Object bean) {
        return BeanReflection.getBeanProperties(bean);
    }

    /**
     * This method returns all bean properties for the specified bean.
     *
     * @param bean The object instance to read the bean properties of.
     * @return A map of all bean property names mapping to the corresponding property values.
     * @see BeanReflection#getPropertyValues(Object)
     */
    public static Map<String, Object> getPropertyValues(Object bean) {
        return BeanReflection.getPropertyValues(bean);
    }

    /**
     * This method creates a new bean instance of the requested type using the default constructor and attempts to
     * set all the specified property values.
     *
     * @param beanType       The bean type to be instantiated using the default constructor.
     * @param propertyValues The properties that have to be set.
     * @param <T>            The bean type to be returned.
     * @return The instantiated bean with the specified property values.
     * @see BeanReflection#createBean(Class, Map)
     */
    public static <T> T createBean(Class<T> beanType, Map<String, Object> propertyValues) {
        return createBean(beanType, propertyValues);
    }

}
