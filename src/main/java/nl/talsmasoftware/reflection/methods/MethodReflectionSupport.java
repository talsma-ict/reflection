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

import nl.talsmasoftware.reflection.classes.ClassReflectionSupport;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for all kinds of reflection utility function that deal with {@link Method java Methods}.
 *
 * @author <a href="mailto:info@talsma-software.nl">Sjoerd Talsma</a>
 */
public final class MethodReflectionSupport {

    private static final Logger LOGGER = Logger.getLogger(MethodReflectionSupport.class.getName());

    private MethodReflectionSupport() {
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
     */
    public static Method findMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            final Class<?> foundClass = ClassReflectionSupport.findClass(className);
            return foundClass != null ? foundClass.getMethod(methodName, parameterTypes) : null;
        } catch (NoSuchMethodException nsme) {
            LOGGER.log(Level.FINEST, "Method \"{1}\" was not found in class \"{0}\".",
                    new Object[]{className, methodName, parameterTypes, nsme.getMessage(), nsme});
        } catch (RuntimeException rte) {
            LOGGER.log(Level.FINEST, "Unexpected exception looking for method \"{1}\" in class \"{0}\": {3}",
                    new Object[]{className, methodName, parameterTypes, rte.getMessage(), rte});
        }
        return null;
    }

}
