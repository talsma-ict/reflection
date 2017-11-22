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
import nl.talsmasoftware.reflection.errorhandling.MissingMethodException;
import nl.talsmasoftware.reflection.errorhandling.ReflectionException;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.talsmasoftware.reflection.Classes.NO_TYPES;
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

    /**
     * Cache, similar to the Introspector method cache.
     */
    private static final Map<Class<?>, Reference<Method[]>> DECLARED_METHOD_CACHE =
            new WeakHashMap<Class<?>, Reference<Method[]>>();

    private Methods() {
        throw new UnsupportedOperationException();
    }

    public static Method getMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null) {
            throw new MissingMethodException("Method \"" + name + "\" cannot be obtained from class <null>.");
        }
        try {

            return type.getMethod(name, parameterTypes);

        } catch (NoSuchMethodException nsme) {
            return findBestMatchingMethod(nsme, type, name, parameterTypes);
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
        if (qualifiedMethodName == null) throw new MissingMethodException("Cannot locate method named <null>.");
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

    public static Method getMethod(Class<?> type, String methodName) {
        return getMethod(type, methodName, NO_TYPES);
    }

    public static Method getMethod(String qualifiedMethodName) {
        return getMethod(qualifiedMethodName, NO_TYPES);
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
     * @param qualifiedMethodName The fully-qualified name of the method to find (full classname + "." + methodName).
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

    public static Method findMethod(Class<?> type, String methodName, String... parameterTypeNames) {
        return findMethod(type, methodName, Classes.findClasses(parameterTypeNames));
    }

    public static Method findMethod(String qualifiedMethodName, String... parameterTypeNames) {
        return findMethod(qualifiedMethodName, Classes.findClasses(parameterTypeNames));
    }

    public static Method findMethod(Class<?> type, String methodName) {
        return findMethod(type, methodName, NO_TYPES);
    }

    public static Method findMethod(String qualifiedMethodName) {
        return findMethod(qualifiedMethodName, NO_TYPES);
    }

    /**
     * This function calls the method for the given <code>subject</code> with the specified <code>parameters</code>.
     * The subject may be <code>null</code> for static methods.
     *
     * @param method     The method to be called (required, non-<code>null</code>).
     * @param subject    The subject to call the method on. (optional, can be <code>null</code> for static methods).
     * @param parameters The parameters to invoke the method with.
     * @param <T>        The expected result type.
     * @return The method result.
     * @throws MethodInvocationException In case there were exceptions when calling the method.
     */
    public static <T> T call(Method method, Object subject, Object... parameters) {
        try {
            if (method == null) {
                throw new MethodInvocationException("Cannot invoke method <null>.");
            } else if (subject == null && !Modifier.isStatic(method.getModifiers())) {
                throw new MethodInvocationException("Cannot invoke non-static method \"" + fqn(method) + "\" on subject <null>.");
            }

            @SuppressWarnings("unchecked") final T result = (T) method.invoke(subject, parameters);
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
     * Returns {@link Class#getDeclaredMethods()} while trying to maintain the actual declaration order from the class
     * file.
     * <p>
     * This is not guaranteed to work or result in more sensible ordering.
     * As fallback, the method order is returned as-is by the class.
     *
     * @param type The type to return the {@code declared methods} for.
     * @return The declared methods for the type.
     */
    public static Method[] getDeclaredMethods(Class type) {
        Method[] result = rawDeclaredMethodsOf(type);
        if (result != null) result = result.clone();
        return result;
    }

    /**
     * The cached, raw and uncloned declared methods.
     * <p>
     * <strong>Important:</strong> do not expose outside of this class.
     */
    private static Method[] rawDeclaredMethodsOf(Class type) {
        Method[] methods = null;
        if (type != null) {
            synchronized (DECLARED_METHOD_CACHE) {
                Reference<Method[]> reference = DECLARED_METHOD_CACHE.get(type);
                if (reference != null) methods = reference.get();
            }
            if (methods == null) {
                methods = reflectDeclaredMethodsFor(type);
                synchronized (DECLARED_METHOD_CACHE) {
                    DECLARED_METHOD_CACHE.put(type, new WeakReference<Method[]>(methods));
                }
            }
        }
        return methods;
    }

    /**
     * @param method The method to return the fully-qualified-name of.
     * @return The fully qualified name (including declaring type) of the method,
     * or <code>"&lt;null&gt;"</code> if no method was passed.
     */
    private static String fqn(Method method) {
        return method == null ? "<null>" : method.getClass().getName() + "." + method.getName();
    }

    private static Method findBestMatchingMethod(NoSuchMethodException nsme, Class<?> type, String name, Class<?>... parameterTypes) {
        // TODO: Smart parameter search strategies.
        for (Method method : allMethodsNamed(type, name)) if (methodMatches(method, parameterTypes)) return method;
        throw new MissingMethodException("Method \"" + name + "\" was not found in " + type + ".", nsme);
    }

    private static Set<Method> allMethodsNamed(Class<?> type, String name) {
        Set<Method> allMethods = new LinkedHashSet<Method>();
        while (type != null) {
            for (Method method : rawDeclaredMethodsOf(type)) if (method.getName().equals(name)) allMethods.add(method);
            type = type.getSuperclass();
        }
        return allMethods;
    }

    private static boolean methodMatches(Method method, Class<?>... parameterTypes) {
        // TODO account for vararg methods.
        Class<?>[] methodTypes = method.getParameterTypes();
        if (methodTypes.length != parameterTypes.length) return false;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] != null && !methodTypes[i].isAssignableFrom(parameterTypes[i])) return false;
            else if (parameterTypes[i] == null && methodTypes[i].isPrimitive()) return false;
        }
        return true;
    }

    private static Method[] reflectDeclaredMethodsFor(Class type) {
        Method[] methods = type.getDeclaredMethods();
        try {
            String rawClassString = readClass(type);
            if (rawClassString != null) {
                // Get the part between the LineNumberTable and SourceFile from the class.
                int idx = rawClassString.indexOf("LineNumberTable");
                if (idx >= 0) rawClassString = rawClassString.substring(idx + "LineNumberTable".length() + 3);
                idx = rawClassString.lastIndexOf("SourceFile");
                if (idx >= 0) rawClassString = rawClassString.substring(0, idx);

                MethodPosition[] offsets = calculateMethodPositions(rawClassString, methods);
                Arrays.sort(offsets);
                for (int i = 0; i < offsets.length; ++i) methods[i] = offsets[i].method;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, "Exception sorting methods by declararion order: " + ex.getMessage(), ex);
        }
        return methods;
    }

    private static String readClass(Class type) throws IOException {
        String classResource = type.getName().replace('.', '/') + ".class";
        InputStream in = type.getClassLoader().getResourceAsStream(classResource);
        if (in == null) return null;
        try {
            Reader reader = new InputStreamReader(in, "UTF-8");
            StringWriter writer = new StringWriter();
            char[] buf = new char[1024];
            for (int read = reader.read(buf); read >= 0; read = reader.read(buf)) writer.write(buf, 0, read);
            return writer.toString();
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                LOGGER.log(Level.FINER, "Couldn't close stream to " + type + ": " + ioe.getMessage(), ioe);
            }
        }
    }

    private static MethodPosition[] calculateMethodPositions(String rawClassData, Method[] methods) {
        // Sort by method.name length
        Arrays.sort(methods, new Comparator<Method>() {
            public int compare(Method a, Method b) {
                return Integer.signum(b.getName().length() - a.getName().length());
            }
        });

        MethodPosition positions[] = new MethodPosition[methods.length];
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            int pos = rawClassData.indexOf(methodName);
            while (pos >= 0) {
                boolean subset = false;
                for (int j = 0; j < i && !subset; j++)
                    subset = positions[j].pos >= 0
                            && positions[j].pos <= pos
                            && pos < positions[j].pos + positions[j].method.getName().length();
                if (!subset) break;
                pos = rawClassData.indexOf(methodName, pos + methodName.length());
            }
            positions[i] = new MethodPosition(methods[i], pos);
        }
        return positions;
    }

    // Container that compares methods based on their position in the raw class.
    private static final class MethodPosition implements Comparable<MethodPosition> {
        private final Method method;
        private final int pos;

        private MethodPosition(Method method, int pos) {
            this.method = method;
            this.pos = pos;
        }

        public int compareTo(MethodPosition target) {
            return Integer.signum(this.pos - target.pos);
        }
    }
}
