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

package nl.talsmasoftware.reflection;

import nl.talsmasoftware.reflection.errorhandling.MethodInvocationException;
import nl.talsmasoftware.reflection.errorhandling.MissingMethodException;
import nl.talsmasoftware.reflection.errorhandling.ReflectionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.talsmasoftware.reflection.Classes.typesOf;

/**
 * A utility class for all kinds of reflection utility function that deal with {@link Method java Methods}.
 * <p>
 * For most functions in this utility class there will be two variants; a 'getXyz' and a 'findXyz' variant.
 * The 'get' function will throw a subclass of {@link ReflectionException} with some explanation when the result
 * cannot be returned.<br>
 * The 'find' variant on the other hand will log such an explanation instead and return <code>null</code> to the caller.
 *
 * @author Sjoerd Talsma
 */
public final class Methods {

    private static final Logger LOGGER = Logger.getLogger(Methods.class.getName());

    private Methods() {
        throw new UnsupportedOperationException();
    }

    public static Method getMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null) {
            throw new MissingMethodException("Method \"" + name + "\" cannot be obtained from class <null>.");
        }
        try {
            // TODO: Smart parameter search strategies.
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException nsme) {
            throw new MissingMethodException("Method \"" + name + "\" was not found in " + type + ".", nsme);
        } catch (RuntimeException rte) {
            throw new MissingMethodException("Unexpected exception looking for method \"" + name + "\" in " + type + ": " + rte.getMessage(), rte);
        }
    }

    /**
     * This function will get a method in a particular class based on a visible signature that matches the specified
     * parameter types.
     * <p>
     * Exceptions during the method reflection will be converted into a {@link MethodInvocationException} and re-thrown.
     * <p>
     * Since normally the methods contained in a normal class do not change, it is advised to keep a reference to the
     * resulting Method around if you intend to re-use it in a short while from now.
     *
     * @param qualifiedMethodName The fully-qualified name of the method to find (full classname + "." + fqn).
     * @param parameterTypes      The parameter types you wish to pass to the class.
     * @return The found method or <code>null</code> if it could not be found.
     * @throws MethodInvocationException if the requested Method could not be found.
     * @see #findMethod(String, Class[])
     */
    public static Method getMethod(String qualifiedMethodName, Class<?>... parameterTypes) {
        if (qualifiedMethodName == null) throw new MethodInvocationException("Cannot locate method named <null>.");
        final int lastDotIdx = qualifiedMethodName.lastIndexOf('.');
        if (lastDotIdx < 1) {
            throw new MissingMethodException("Method name does not contain both a type and method name: \"" +
                    qualifiedMethodName + "\".");
        }
        return getMethod(Classes.getClass(qualifiedMethodName.substring(0, lastDotIdx)),
                qualifiedMethodName.substring(lastDotIdx + 1), parameterTypes);
    }

    public static Method getMethod(String qualifiedMethodName, String... parameterTypes) {
        return getMethod(qualifiedMethodName, Classes.getClasses(parameterTypes));
    }

    public static Method getMethod(Class<?> type, String methodName, String... parameterTypeNames) {
        return getMethod(type, methodName, Classes.getClasses(parameterTypeNames));
    }

    public static Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return getMethod(type, methodName, parameterTypes);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    /**
     * This function will find a method in a particular class based on a visible signature that matches the specified
     * parameter types.
     * <p>
     * Exceptions during the method reflection will be traced to a logger and <code>null</code> will be returned
     * indicating that the method could not be found.
     * <p>
     * Since normally the methods contained in any class do not change, it is advised to keep a reference to the
     * resulting Method around if you intend to re-use it in a short while from now.
     *
     * @param qualifiedMethodName The fully-qualified name of the method to find (full classname + "." + fqn).
     * @param parameterTypes      The parameter types you wish to pass to the class.
     * @return The found method or <code>null</code> if it could not be found.
     * @see #getMethod(String, Class[])
     */
    public static Method findMethod(String qualifiedMethodName, Class<?>... parameterTypes) {
        try {
            return getMethod(qualifiedMethodName, parameterTypes);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    /**
     * Deze operatie roept de methode aan voor het opgegeven <code>subject</code>
     * (mag <code>null</code> zijn voor statische methodes) en de opegeven <code>parameters</code>.
     *
     * @param method     De aan te roepen methode (optioneel, de call doet niets indien <code>null</code>).
     * @param subject    Het object om de methode op aan te roepen (optioneel, mag <code>null</code> zijn voor statische methodes).
     * @param parameters De te gebruiken parameters voor de methode.
     * @param <T>        Het verwachte resultaat type.
     * @return Het resultaat van de methode of <code>null</code> indien er geen methode werd meegegeven
     * of er een exceptie optrad tijdens de aanroep.
     */
    public static <T> T call(Method method, Object subject, Object... parameters) {
        try {
            if (method == null) throw new MethodInvocationException("Cannot invoke method <null>.");
            else if (subject == null && !Modifier.isStatic(method.getModifiers())) {
                throw new MethodInvocationException("Cannot invoke non-static method \"" + fqn(method) + "\" on subject <null>.");
            }

            @SuppressWarnings("unchecked")
            final T result = (T) method.invoke(subject, parameters);
            return result;

        } catch (InvocationTargetException ite) {
            final Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            throw new MethodInvocationException("Method \"" + fqn(method) + "\" threw exception: " + cause.getMessage(), ite);
        } catch (IllegalAccessException iae) {
            throw new MethodInvocationException("Not allowed to call method \"" + fqn(method) + "\": " + iae.getMessage(), iae);
        }
    }

    public static Object call(String methodName, Object subject, Object... parameters) {
        if (methodName != null && methodName.indexOf('.') >= 0) { // Consider method name to be fully-qualified.
            return call(getMethod(methodName, typesOf(parameters)), subject, parameters);
        } else if (subject == null) throw new MethodInvocationException(
                "Cannot determine declaring class for method \"" + methodName + "\", subject was <null>.");
        // final Class<?> declaringClass = subject instanceof Class ? (Class<?>) subject : subject.getClass();
        return call(getMethod(subject.getClass(), methodName, typesOf(parameters)), subject, parameters);
    }

    public static <T> T callStatic(Method method, Object... parameters) {
        return call(method, null, parameters);
    }

    public static Object callStatic(String qualifiedMethodName, Object... parameters) {
        return call(qualifiedMethodName, null, parameters);
    }

    public static <T> T tryCall(Method method, Object subject, Object... parameters) {
        try {
            return call(method, subject, parameters);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    public static Object tryCall(String methodName, Object subject, Object... parameters) {
        try {
            return call(methodName, subject, parameters);
        } catch (ReflectionException re) {
            LOGGER.log(Level.FINEST, re.getMessage(), re);
            return null;
        }
    }

    public static <T> T tryCallStatic(Method method, Object... parameters) {
        return tryCall(method, null, parameters);
    }

    public static Object tryCallStatic(String qualifiedMethodName, Object... parameters) {
        return tryCall(qualifiedMethodName, null, parameters);
    }

    /**
     * @param method The method to return the fully-qualified-name of.
     * @return The fully qualified name (including declaring type) of the method,
     * or <code>"&lt;null&gt;"</code> if no method was passed.
     */
    private static String fqn(Method method) {
        return method == null ? "<null>" : method.getClass().getName() + "." + method.getName();
    }
}
