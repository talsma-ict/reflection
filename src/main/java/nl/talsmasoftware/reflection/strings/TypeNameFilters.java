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
package nl.talsmasoftware.reflection.strings;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to provide pluggable ability to filter the name of a type.
 * <p>
 * This functionality is used in the {@code ToStringBuilder prefix} initialization.
 *
 * @author Sjoerd Talsma
 */
public final class TypeNameFilters {
    private static final Logger LOGGER = Logger.getLogger(TypeNameFilters.class.getName());

    private static TypeNameFilter filter;

    private TypeNameFilters() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * This method returns the {@link Class#getSimpleName()} for the given type.
     *
     * @param typeName The name of the type to return either filtered or as-is.
     * @return The (possibly filtered) name of the type.
     */
    public static String filter(String typeName) {
        TypeNameFilter f = filter; // Assigning to f allows for reset even between initFilter and apply
        if (f == null) filter = f = initFilter();
        return f.apply(typeName);
    }

    /**
     * Resets the detected filters, triggering lookup of new filters at the next call to {@link #filter(String)}.
     */
    public static void reset() {
        filter = null;
    }

    /**
     * Tries to load a filter through the {@code ServiceLoader}.
     * This obviously only works on a Java 6 JVM, which is a reasonable assumption to make.
     * If no filter is found, the {@link TypeNameFilter#IDENTITY} constant is simply returned.
     *
     * @return The initialized filter.
     */
    private static TypeNameFilter initFilter() {
        TypeNameFilter f = null;
        try {
            for (TypeNameFilter foundFilter : java.util.ServiceLoader.load(TypeNameFilter.class)) {
                if (f == null) {
                    f = foundFilter;
                    continue;
                }
                final TypeNameFilter current = f, found = foundFilter;
                f = new TypeNameFilter() {
                    public String apply(String typeName) {
                        return found.apply(current.apply(typeName));
                    }
                };
            }
        } catch (LinkageError noServiceLoader) {
            LOGGER.log(Level.FINE, "No ServiceLoader found, probably still running on Java 5.", noServiceLoader);
        } catch (Exception errorLoading) {
            LOGGER.log(Level.FINE, "Couldn't load TypeNameFilters.TypeNameFilter from serviceloader.", errorLoading);
        }
        return f == null ? TypeNameFilter.IDENTITY : f;
    }

}
