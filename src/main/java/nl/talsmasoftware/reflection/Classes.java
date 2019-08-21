/*
 * Copyright 2016-2019 Talsma ICT
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

import nl.talsmasoftware.reflection.errorhandling.InstantiatingException;
import nl.talsmasoftware.reflection.errorhandling.MissingClassException;
import nl.talsmasoftware.reflection.errorhandling.ReflectionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection support for finding classes and interacting with them.
 *
 * @author Sjoerd Talsma
 */
public final class Classes {
    private static final Logger LOGGER = Logger.getLogger(Classes.class.getName());
    static final Object[] NO_PARAMS = new Object[0];
    static final Class[] NO_TYPES = new Class[0];

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private Classes() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method attempts to load the requested class from the classpath.
     * <p>
     * It is also possible to {@link #getClasses(String...) load multiple classes at once}.
     *
     * @param className The fully qualified name of the class to be loaded.
     * @return The loaded class (non-<code>null</code>).
     * @throws MissingClassException In case the requested class cannot be found.
     * @see #findClass(String)
     */
    public static Class<?> getClass(String className) {
        try {

            // TODO: Apply some general (defensive but correct) classloader strategy!
            if (className == null) throw new IllegalArgumentException("Classname is required to get a class instance.");
            return Class.forName(className);

        } catch (java.lang.ClassNotFoundException cnfe) {
            throw new MissingClassException("Class \"" + className + "\" not found. " +
                    "It is likely that some JAR library is not available on the classpath.", cnfe);
        } catch (LinkageError le) {
            throw new MissingClassException("Linkage error while loading \"" + className + "\" class. " +
                    "It is likely that there is an incompatible version of some JAR library on the classpath.", le);
        } catch (RuntimeException rte) {
            throw new MissingClassException("Unexpected exception attempting to load class \"" + className + "\": "
                    + rte.getMessage(), rte);
        }
    }

    /**
     * This method attempts to load multiple classes at once, delegating to {@link #getClass(String)}.
     *
     * @param classNames The names of the classes to be loaded (may not contain <code>null</code> String values).
     * @return The loaded classes (non-<code>null</code> array containing only non-<code>null</code> classes).
     * @throws MissingClassException In case any of the requested classes could not be loaded.
     * @see #findClasses(String...)
     */
    public static Class[] getClasses(String... classNames) {
        if (classNames == null || classNames.length == 0) return NO_TYPES;
        final Class[] classes = new Class[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            classes[i] = getClass(classNames[i]);
        }
        return classes;
    }

    /**
     * This method attempts to {@link #getClass(String) get the requested class and return it}, but in case of any
     * exception, it will be logged and the method will simply return <code>null</code>.
     *
     * @param className The name of the class to find.
     * @return The found class or <code>null</code> if it could not be found.
     * @see #getClass(String)
     */
    public static Class<?> findClass(String className) {
        try {
            return getClass(className);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    public static Class[] findClasses(String... classNames) {
        if (classNames == null || classNames.length == 0) return NO_TYPES;
        final Class[] classes = new Class[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            classes[i] = findClass(classNames[i]);
        }
        return classes;
    }

    /**
     * @param type                  The type of the object to be instantiated.
     * @param constructorParameters The parameters to invoke the constructor with.
     * @param <T>                   The type of the object to be instantiated.
     * @return The non-<code>null</code> object instance of the specified class.
     * @throws InstantiatingException In case the called constructor threw an exception.
     */
    public static <T> T createNew(Class<T> type, Object... constructorParameters) {
        try {

            @SuppressWarnings("unchecked") final Constructor<T> constructor = Constructors.getConstructor(type, typesOf(constructorParameters));
            return constructor.newInstance(constructorParameters);

        } catch (InvocationTargetException ite) {
            final Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            throw new InstantiatingException("Exception thrown from constructor of " + type + ": " + cause.getMessage(), ite);
        } catch (InstantiationException ie) {
            final String abstr = Modifier.isAbstract(type.getModifiers()) ? "abstract " : "";
            throw new InstantiatingException("Cannot instantiate " + abstr + type + ".", ie);
        } catch (IllegalAccessException e) {
            throw new InstantiatingException("Not allowed to instantiate object of " + type + ".", e);
        }
    }

    public static <T> T createNew(Class<T> type) {
        return createNew(type, NO_PARAMS);
    }

    /**
     * @param className             The qualified name of the class to be instantiated.
     * @param constructorParameters The parameters to invoke the constructor with.
     * @return The non-<code>null</code> object instance of the specified class.
     * @throws InstantiatingException In case the called constructor threw an exception.
     */
    public static Object createNew(String className, Object... constructorParameters) {
        return createNew(getClass(className), constructorParameters);
    }

    public static Object createNew(String className) {
        return createNew(getClass(className), NO_PARAMS);
    }

    public static <T> T tryCreateNew(Class<T> type, Object... constructorParameters) {
        try {
            return createNew(type, constructorParameters);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    public static <T> T tryCreateNew(Class<T> type) {
        return tryCreateNew(type, NO_PARAMS);
    }

    /**
     * Tries to create a new object instance for the specified class but returns <code>null</code> in case any
     * exceptions occurred.
     *
     * @param className             The qualified name of the class to be instantiated.
     * @param constructorParameters The parameters to invoke the constructor with.
     * @return The created object or <code>null</code> in case any exception occurred.
     */
    public static Object tryCreateNew(String className, Object... constructorParameters) {
        try {
            return createNew(className, constructorParameters);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    public static Object tryCreateNew(String className) {
        return tryCreateNew(className, NO_PARAMS);
    }

    static Class[] typesOf(Object... args) {
        if (args == null || args.length == 0) return NO_TYPES;
        final Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

}
