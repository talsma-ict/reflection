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

import java.lang.reflect.Type;

/**
 * Interface that declares how a particular bean property can be accessed.
 *
 * @author <a href="mailto:info@talsma-software.nl">Sjoerd Talsma</a>
 */
public interface BeanProperty {

    /**
     * @return The name of the bean property.
     */
    String getName();

    /**
     * @return The type of the bean property.
     */
    Type getType();

    /**
     * @return Whether or not the bean property can be read.
     */
    boolean isReadable();

    /**
     * @return Whether or not the bean property can be weritten.
     */
    boolean isWriteable();

    /**
     * Reads the property from a bean instance.
     *
     * @param bean The bean instance to read this property from.
     * @return The value of this property from the specified bean
     * or <code>null</code> if this property could not be read from the specified bean instance.
     */
    Object read(Object bean);

    /**
     * Writes the property into a bean instance.
     *
     * @param bean          The bean instance to write this property to.
     * @param propertyValue The value to be written into this property for the specified bean.
     * @return <code>true</code>if the property was successfully written into the bean
     * or <code>false</code> it that was not possible for some reason.
     */
    boolean write(Object bean, Object propertyValue);

}
