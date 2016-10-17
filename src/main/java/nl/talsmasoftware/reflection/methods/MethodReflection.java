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

package nl.talsmasoftware.reflection.methods;

import nl.talsmasoftware.reflection.classes.ClassReflection;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for all kinds of reflection utility function that deal with {@link Method java Methods}.
 * <p>
 * For most functions in this utility class there will be two variants; a 'getXyz' and a 'findXyz' variant. The 'get'
 * function will throw a subclass of RuntimeException with some explanation when the result cannot be returned.
 * The 'find' variant on the other hand will log such an explanation and instead return <code>null</code> to the caller.
 *
 * @author Sjoerd Talsma
 */
public final class MethodReflection {

    private static final Logger LOGGER = Logger.getLogger(MethodReflection.class.getName());

    private MethodReflection() {
        throw new UnsupportedOperationException();
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
     * @param className      The (fully qualified) name of the class to load the method from.
     * @param methodName     The name of the method to find.
     * @param parameterTypes The parameter types you wish to pass to the class.
     * @return The found method or <code>null</code> if it could not be found.
     * @see #getMethod(String, String, Class[])
     */
    public static Method findMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            return getMethod(className, methodName, parameterTypes);
        } catch (MethodNotFoundException mnfe) {
            LOGGER.log(Level.FINEST, mnfe.getMessage(), mnfe);
            return null;
        }
    }

    /**
     * This function will get a method in a particular class based on a visible signature that matches the specified
     * parameter types.
     * <p>
     * Exceptions during the method reflection will be converted into a {@link MethodNotFoundException} and re-thrown.
     * <p>
     * Since normally the methods contained in a normal class do not change, it is advised to keep a reference to the
     * resulting Method around if you intend to re-use it in a short while from now.
     *
     * @param className      The (fully qualified) name of the class to load the method from.
     * @param methodName     The name of the method to find.
     * @param parameterTypes The parameter types you wish to pass to the class.
     * @return The found method or <code>null</code> if it could not be found.
     * @throws MethodNotFoundException if the requested Method could not be found.
     * @see #findMethod(String, String, Class[])
     */
    public static Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            // TODO: Smart parameter search strategies.
            final Class<?> foundClass = ClassReflection.getClass(className);
            if (foundClass == null) throw new IllegalStateException("Class \"" + className + "\" not found!");
            return foundClass.getMethod(methodName, parameterTypes);
        } catch (ClassReflection.MissingClassException mce) {
            throw new MethodNotFoundException("Class \"" + className + "\" was not found while looking for method \"" +
                    methodName + "\": " + mce.getMessage(), mce);
        } catch (NoSuchMethodException nsme) {
            throw new MethodNotFoundException("Method \"" + methodName + "\" was not found in class \"" + className + "\".", nsme);
        } catch (RuntimeException rte) {
            throw new MethodNotFoundException("Unexpected exception looking for method \"" + methodName +
                    "\" in class \"" + className + "\": " + rte.getMessage(), rte);
        }
    }

    public static class MethodNotFoundException extends IllegalStateException {
        protected MethodNotFoundException(String message, Throwable cause) {
            super(message);
            if (cause != null) super.initCause(cause);
        }
    }
}
