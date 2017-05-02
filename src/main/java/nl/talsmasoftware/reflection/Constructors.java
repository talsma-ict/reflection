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
package nl.talsmasoftware.reflection;

import nl.talsmasoftware.reflection.errorhandling.MethodInvocationException;
import nl.talsmasoftware.reflection.errorhandling.MissingConstructorException;
import nl.talsmasoftware.reflection.errorhandling.ReflectionException;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.talsmasoftware.reflection.Classes.NO_TYPES;

/**
 * A utility class for all kinds of reflection utility function that deal with {@link Constructor class constructors}.
 *
 * @author Sjoerd Talsma
 */
public final class Constructors {

    private static final Logger LOGGER = Logger.getLogger(Constructors.class.getName());

    private Constructors() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param type           The class to obtain the constructor for.
     * @param parameterTypes The parameter types the constructor needs to be called with.
     * @param <T>            The type of object created by the returned constructor.
     * @return The constructor that accepts the specified parameter types.
     * @throws MethodInvocationException If no suitable constructor could be found corresponding to the parameter types.
     */
    public static <T> Constructor<T> getConstructor(Class<T> type, Class<?>... parameterTypes) {
        if (type == null) throw new MethodInvocationException("Cannot obtain constructor for class <null>.");
        try {

            // TODO: findConstructor based on the actual arguments object-array.
            return type.getConstructor(parameterTypes);

        } catch (NoSuchMethodException nsme) {
            throw new MissingConstructorException("Appropriate constructor for " + type + " with parameter types "
                    + Arrays.asList(parameterTypes) + " not found.", nsme);
        } catch (RuntimeException rte) {
            throw new MethodInvocationException("Unexpected exception looking for constructor of " + type + ": "
                    + rte.getMessage(), rte);
        }
    }

    public static Constructor<?> getConstructor(String className, Class<?>... parameterTypes) {
        return getConstructor(Classes.getClass(className), parameterTypes);
    }

    public static Constructor<?> getConstructor(String className, String... parameterTypeNames) {
        return getConstructor(Classes.getClass(className), Classes.getClasses(parameterTypeNames));
    }

    public static Constructor<?> getConstructor(String className) {
        return getConstructor(Classes.getClass(className), NO_TYPES);
    }

    public static <T> Constructor<T> findConstructor(Class<T> type, Class<?>... parameterTypes) {
        try {
            return getConstructor(type, parameterTypes);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    public static Constructor<?> findConstructor(String className, Class<?>... parameterTypes) {
        return findConstructor(Classes.findClass(className), parameterTypes);
    }

    public static Constructor<?> findConstructor(String className, String... parameterTypeNames) {
        return findConstructor(Classes.findClass(className), Classes.findClasses(parameterTypeNames));
    }

    public static Constructor<?> findConstructor(String className) {
        return findConstructor(Classes.findClass(className), NO_TYPES);
    }

}
