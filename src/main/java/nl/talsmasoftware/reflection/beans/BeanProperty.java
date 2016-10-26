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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Interface that declares how a particular bean property can be accessed.
 *
 * @author Sjoerd Talsma
 */
public interface BeanProperty {

    /**
     * The name of the bean property. This is either the field name or the name of the property deduced from a
     * JavaBean accessor method (i.e. <code>getValue()</code> results in a property named <code>"value"</code>).
     *
     * @return The name of the bean property.
     */
    String getName();

    /**
     * The Java {@link Type} of the value contained in the bean property.
     *
     * @return The type of the bean property.
     */
    Type getType();

    /**
     * Indication whether the property can be read. Some properties can only be written,
     * because there may only a 'setter' property accessor method defined.
     *
     * @return Whether or not the bean property can be read.
     */
    boolean isReadable();

    /**
     * Indication whether the property can be written. Some properties may only be read,
     * for example <code>final</code> fields or fields with only a 'getter' property accessor
     * method defined.
     *
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

    /**
     * This method returns any declared annotations for the property (e.g. field or getter/setter methods).
     *
     * @return The annotations declared for the property.
     */
    Collection<Annotation> annotations();

}
