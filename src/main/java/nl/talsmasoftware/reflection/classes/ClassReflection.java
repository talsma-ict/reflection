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

package nl.talsmasoftware.reflection.classes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection support for finding and interacting with classes.
 *
 * @author Sjoerd Talsma
 */
public final class ClassReflection {
    private static final Logger LOGGER = Logger.getLogger(ClassReflection.class.getName());
    private static final Object[] NO_PARAMS = new Object[0];
    private static final Class[] NO_TYPES = new Class[0];

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ClassReflection() {
        throw new UnsupportedOperationException();
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
        } catch (MissingClassException mce) {
            LOGGER.log(Level.FINEST, mce.getMessage(), mce);
            return null;
        }
    }

    public static Class<?> getClass(String className) {
        try {

            // TODO: Apply some general (defensive but correct) classloader strategy!
            return Class.forName(className);

        } catch (java.lang.ClassNotFoundException cnfe) {
            throw new MissingClassException("Class \"" + className + "\" not found. " +
                    "It is likely that some JAR library is not available on the classpath.", cnfe);
        } catch (LinkageError le) {
            throw new MissingClassException("Linkage error while loading \"" + className + "\" class. " +
                    "It is likely that there is an incompatible version of some JAR library on the classpath.", le);
        } catch (RuntimeException rte) {
            throw new MissingClassException("Unexpected exception attempting to load class \"" + className + "\": "
                    + rte, rte);
        }

    }

    public static Class[] findClasses(String... classNames) {
        if (classNames == null || classNames.length == 0) return NO_TYPES;
        final Class[] classes = new Class[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            classes[i] = findClass(classNames[i]);
            // TODO: Think about wanted behaviour in this case..
//            if (classes[i] == null) throw new IllegalArgumentException("Class not found: " + classNames[i]);
        }
        return classes;
    }

    public static <T> T createNew(String className) {
        return createNew(className, NO_PARAMS);
    }

    // TODO: findConstructor based on the actual arguments object-array.
    protected static <T> T createNew(String className, Object... constructorParameters) {
        try {
            final Constructor<T> constructor = findConstructor(className, typesOf(constructorParameters));
            if (constructor == null) throw new IllegalStateException(
                    "No suitable constructor found for class \"" + className + "\".");
            return constructor.newInstance(constructorParameters);
        } catch (InvocationTargetException ite) {
            final Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            throw cause instanceof RuntimeException ? (RuntimeException) cause : new IllegalStateException(
                    "Fout bij aanmaken nieuw object van \"" + className + "\" klasse: " + cause.getMessage(), cause);
//        } catch (ReflectiveOperationException | LinkageError e) {
//            throw new IllegalStateException(
//                    "Reflectiefout bij aanmaken nieuw \"" + className + "\" object: " + e.getMessage(), e);
        } catch (InstantiationException e) {
//            e.printStackTrace();
            throw new UnsupportedOperationException("TODO proper error handling.", e);
        } catch (IllegalAccessException e) {
//            e.printStackTrace();
            throw new UnsupportedOperationException("TODO proper error handling.", e);
        }
    }

    private static Class[] typesOf(Object... args) {
        if (args == null || args.length == 0) return NO_TYPES;
        final Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

    @SuppressWarnings("unchecked")
    protected static <T> Constructor<T> findConstructor(String className, Class<?>... paramTypes) {
        try {
            // TODO: Similar to methods, find 'most suitable' constructor and support null types.

            return (Constructor<T>) findClass(className).getConstructor(paramTypes);

//        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
//            LOGGER.log(Level.FINEST, "Geen toepasselijke constructor gevonden voor klasse \"{0}\" " +
//                            "die overeenkomt met de argumenten {1}.",
//                    new Object[]{className, Arrays.asList(constructorParams), e});
//            return null;
        } catch (NoSuchMethodException e) {
            // TODO Logging!
        }
        return null;
    }

    public static class MissingClassException extends IllegalStateException {
        protected MissingClassException(String message, Throwable cause) {
            super(message);
            if (cause != null) super.initCause(cause);
        }
    }

}
