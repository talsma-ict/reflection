/*
 * Copyright 2016-2022 Talsma ICT
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

/**
 * Filter for type name.
 * <p>
 * A filter can be applied by creating an implementation and declaring it in a file called
 * <code>/META-INF/services/nl.talsmasoftware.reflection.strings.TypeNameFilters$TypeNameFilter</code> so it can be picked
 * up by the Java {@code ServiceLoader} when running on (at least) Java 6 environment.
 * <p>
 * All calls to {@link TypeNameFilters#filter(String)} will be fed to the registered filter.
 *
 * @author Sjoerd Talsma
 */
public interface TypeNameFilter {
    /**
     * Constant for 'no filter' or the 'identity function'.
     */
    TypeNameFilter IDENTITY = new TypeNameFilter() {
        public String apply(String typeName) {
            return typeName;
        }
    };

    /**
     * The type name to be filtered.
     *
     * @param typeName The original value (possibly null).
     * @return The filtered value.
     */
    String apply(String typeName);
}
