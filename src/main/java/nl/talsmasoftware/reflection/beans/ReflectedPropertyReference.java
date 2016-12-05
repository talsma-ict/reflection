/*
 * Copyright (C) 2016 Talsma ICT
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
 *
 */

package nl.talsmasoftware.reflection.beans;

/**
 * Class to encapsulate a concrete property combined with a bean containing it.
 *
 * @author Sjoerd Talsma
 */
final class ReflectedPropertyReference {
    private final Object bean;
    private final BeanProperty property;

    ReflectedPropertyReference(Object bean, BeanProperty property) {
        if (bean == null) throw new NullPointerException(
                "Cannot instantiate reflected property reference for bean <null>.");
        if (property == null) throw new NullPointerException(
                "Cannot instantiate reference without reflected property for bean: " + bean + ".");
        this.bean = bean;
        this.property = property;
    }

    Object read() {
        return property.read(bean);
    }

    boolean write(final Object value) {
        return property.write(bean, value);
    }

}
