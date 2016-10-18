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

/**
 * The main class in this package is
 * {@link nl.talsmasoftware.reflection.beans.BeanReflection BeanReflection} providing static utility
 * methods that enable reflection of JavaBean properties.
 * <p>
 * This package conforms to the JavaBean convention, with one important distinction: support for public fields.
 * A property can be 'backed' by getter/setter methods, but also by a public field. The field is used directly when
 * an appropriate getter/setter method cannot be found.
 * <p>
 * Class diagram for this package:<br><center><img src="package.svg" alt="package class diagram"></center>
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.reflection.beans;