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

import nl.talsmasoftware.reflection.beans.BeanReflection;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A builder for 'toString' for objects that can append their named or unnamed properties to this builder.
 * <p>
 * The builder is very customizable, but has some sensible defaults:
 * <ul>
 * <li>Any result wil start with some 'prefix' which by default is the simple name of the class being handled.</li>
 * <li>All appended properties will be included within brackets, by default <code>'{'</code> and <code>'}'</code>.</li>
 * <li>Properties will by default be separated from each-other by the separator string <code>", "</code>.</li>
 * <li><code>null</code> values will by default not be included.</li>
 * <li><code>brackets</code> will by default only be added to the prefix if there is content within them.</li>
 * </ul>
 * <p>
 * An example of a default result could be:
 * <code>"TypeName{field=\"String value\", otherField=0, booleanField=false}"</code>.
 * <p>
 * Class diagram:<br><center><img src="ToStringBuilder.svg" alt="class diagram"></center>
 *
 * @author Sjoerd Talsma
 */
public class ToStringBuilder implements Appendable, CharSequence, Serializable {
    private static final long serialVersionUID = 1L;
    private static int maximumValueLength = 128;

    protected final String prefix;
    protected final StringBuilder fields = new StringBuilder();

    // Customizable behaviour for this builder:
    private String leftBracket = "{", separator = ", ", rightBracket = "}";
    private boolean includeNulls = false, forceBrackets = false;

    /**
     * Constructor to create a new ToStringBuilder for a particular object or type.
     *
     * @param source The object or type to create the ToStringBuilder for (Object, Class or a name).
     */
    public ToStringBuilder(Object source) {
        this(null, source);
    }

    /**
     * Constructor to create a new ToStringBuilder for a <code>source</code> object, while copying all customizable
     * behaviour from another <code>parent</code> builder such as: brackets, separator, includeNulls and forceBrackets.
     *
     * @param parent The parent builder to copy settings from (optional, don't copy any settings when <code>null</code>).
     * @param source The object to create the string for (or a custom prefix string).
     */
    protected ToStringBuilder(ToStringBuilder parent, Object source) {
        if (source == null || source instanceof CharSequence) {
            this.prefix = nullToEmpty(source);
        } else {
            this.prefix = TypeNameFilters.filter(classOf(source).getSimpleName());
        }
        if (parent != null) {
            this.leftBracket = parent.leftBracket;
            this.separator = parent.separator;
            this.rightBracket = parent.rightBracket;
            this.includeNulls = parent.includeNulls;
            this.forceBrackets = parent.forceBrackets;
        }
    }

    /**
     * This method creates a {@link ToStringBuilder} where all public fields and property accessor methods from the
     * source object have been reflected.
     *
     * @param source The source object for which a toString() builder will be created based on its public fields and
     *               property accessor methods.
     * @return The builder that already has all public properties included in it, never <code>null</code>.
     */
    public static ToStringBuilder reflect(Object source) {
        ToStringBuilder builder = new ToStringBuilder(source);
        if (source == null || source instanceof CharSequence) return builder; // we're done already
        return builder.appendReflectedPropertiesOf(source);
    }

    /**
     * Use reflection to add all readable properties of the given object to this builder.
     *
     * @param source The object to reflect the public properties for.
     * @return Reference to this builder for method-chaining purposes.
     */
    private ToStringBuilder appendReflectedPropertiesOf(Object source) {
        for (Map.Entry<String, Object> property : BeanReflection.getPropertyValues(source).entrySet()) {
            final String name = property.getKey();
            if (!"class".equals(name)) { // class is already contained in the builder prefix.
                this.append(name, property.getValue());
            }
        }
        return this;
    }

    /**
     * Tells the builder that <code>null</code> properties must be added (which is <code>false</code> by default).
     *
     * @return Reference to this builder for method-chaining with updated 'includeNulls' setting.
     */
    public ToStringBuilder includeNulls() {
        return includeNulls(true);
    }

    /**
     * Tells the builder whether <code>null</code> property values should be appended. (<code>false</code> by default).
     *
     * @param includeNulls Whether <code>null</code> properties should be appended.
     * @return Reference to this builder for method-chaining with updated 'includeNulls' setting.
     */
    public ToStringBuilder includeNulls(boolean includeNulls) {
        this.includeNulls = includeNulls;
        return this;
    }

    /**
     * Configuration method that allows the builder to use another separator between the appended properties
     * (<code>", "</code> by default).
     * <p>
     * Please note that this separator has no influence on previously appended properties.
     *
     * @param separator The new separator that should be used when appending additional properties to this builder,
     *                  or <code>null</code> to omit the separator altogether.
     * @return Reference to this builder for method-chaining with updated 'separator' setting.
     */
    public ToStringBuilder separator(CharSequence separator) {
        this.separator = nullToEmpty(separator);
        return this;
    }

    /**
     * Forces brackets being added to this builder, even if there are no appended properties. The method also specifies
     * which brackets will be used.
     * <p>
     * It is also possible to merely modify the brackets being used without forcing them into the end-result using the
     * {@link #brackets(String, String)} configuration method.
     *
     * @param openingBracket The opening bracket to use for the resulting string.
     * @param closingBracket The closing bracket to use for the resulting string.
     * @return Reference to this builder for method-chaining with updated brackets settings.
     * @see #brackets(String, String)
     */
    public ToStringBuilder forceBrackets(String openingBracket, String closingBracket) {
        this.forceBrackets = true;
        return brackets(openingBracket, closingBracket);
    }

    /**
     * Configuration method that defines a custom set of brackets around the appended properties.
     * <p>
     * It is also possible to force brackets being added even if no properties were added by using the
     * {@link #forceBrackets(String, String)} method.
     *
     * @param openingBracket The opening bracket to use for the resulting string
     *                       (<code>"{"</code> is default, specify <code>null</code> to omit this bracket).
     * @param closingBracket The closing bracket to use for the resulting string
     *                       (<code>"}"</code> is default, specify <code>null</code> to omit this bracket).
     * @return Reference to this builder for method-chainining with updated brackets settings.
     * @see #forceBrackets(String, String)
     */
    public ToStringBuilder brackets(String openingBracket, String closingBracket) {
        this.leftBracket = nullToEmpty(openingBracket);
        this.rightBracket = nullToEmpty(closingBracket);
        return this;
    }

    /**
     * This method attempts to add the appended properties from a finished <code>toString()</code> result to this
     * builder. This could be from a superclass toString result for example.
     * <p>
     * For this parsing the builder will use the configured brackets settings to attempt to find the appended
     * properties.
     * <p>
     * When in control of the superclass, it may be better to 'design it for extension' by letting it return the
     * non-finalized builder in a protected method so it can be appended to by a subclass without having to parse the
     * end-result.<br><br>
     * <code>
     * protected ToStringBuilder toStringBuilder() {<br>
     * return super.toStringBuilder().append("fieldname", "Field value");<br>
     * }
     * </code>
     *
     * @param superToStringResult The toString result from the superclass from which the appended properties will
     *                            be extracted if possible.
     * @return Reference to this builder for method-chaining with the properties from the superclass appended to it.
     */
    public ToStringBuilder appendSuper(CharSequence superToStringResult) {
        return append(new ToStringBetween(superToStringResult, leftBracket, rightBracket));
    }

    /**
     * This method is the only main 'append' method for this builder.
     * All other append calls eventually route back to this method.
     *
     * @param name  The name of the property being added (optional).
     * @param value The value of the property being added.
     * @return Reference to this builder for method-chaining.
     * @see #includeNulls()
     */
    public ToStringBuilder append(final CharSequence name, final Object value) {
        if (includeNulls || value != null) {
            if (fields.length() > 0) {
                fields.append(separator);
            }
            if (name != null && name.length() > 0) {
                fields.append(name).append('=');
            }
            this.appendValue(value);
        }
        return this;
    }

    /**
     * Recursion detector that can detect recursion in 'apendValue' calls from circular references despite the 'size'
     * of the circle, so it does not matter how many nodes are in between the eventual self-reference.
     */
    private static final ThreadLocal<Map<Object, Object>> APPENDVALUE_RECURSION_DETECTOR = new ThreadLocal<Map<Object, Object>>() {
        @Override
        protected Map<Object, Object> initialValue() {
            return new IdentityHashMap<Object, Object>();
        }
    };

    /**
     * Delegate method that appends a property 'value' to this builder.
     * Is is made protected to allow subclasses to map Objects to custom String representations if required.
     * <p>
     * This method gets called <em>after</em> a decision was made to append the value, so it will not be called in vain.
     * <p>
     * The method <em>does not need to take separators or field names into account</em>. That has all been taken care of
     * before this method call by {@link #append(CharSequence, Object)}. Therefore it is not normally necessary to call
     * this method by yourself.
     * <p>
     * Some specifics on how objects are rendered by default:
     * <ul>
     * <li><code>null</code>: <code>&lt;null&gt;</code>.</li>
     * <li>{@link CharSequence}: <code>"value"</code>.</li>
     * <li>any other <code>Object</code>: <code>value.toString()</code>, except:</li>
     * <li>for objects with the 'default' java.lang.Object string representation (class@hash) the
     * {@link #reflect(Object) reflected} String representation is used.</li>
     * </ul>
     *
     * @param value The property value to be added to this builder.
     */
    protected void appendValue(Object value) {
        final Map<Object, Object> sources = APPENDVALUE_RECURSION_DETECTOR.get();
        if (sources.containsKey(value)) { // recursion, ABSOLUTELY DO NOT CALL value.toString() here!
            fields.append(defaultToString(value));
        } else try {
            sources.put(value, value);
            _appendValue(value); // <<-- safe to use our actual implementation method here.
        } finally {
            sources.remove(value);
        }
    }

    /**
     * The standard implementation of {@link #appendValue(Object)} as we want to, without having to consider the
     * recursion problem.
     *
     * @param value The property value to be added to this builder.
     */
    private void _appendValue(Object value) {
        CharSequence stringValue;
        boolean addQuotes = false;
        if (value == null) {
            stringValue = "<null>";
        } else if (value instanceof CharSequence) {
            addQuotes = true;
            stringValue = escapeQuotesInString(value.toString());
        } else {
            stringValue = value.toString();
            if (defaultToString(value).equals(stringValue)) {
                stringValue = new ToStringBuilder(this, value).appendReflectedPropertiesOf(value);
            }
        }

        int length = addQuotes ? ((CharSequence) value).length() : stringValue.length();
        if (length > maximumValueLength) {
            String simpleName = value.getClass().getSimpleName();
            if (simpleName.length() > 0) {
                fields.append('<').append(simpleName).append('>');
            } else {
                fields.append('<').append(length).append("chars>");
            }
        } else if (addQuotes) {
            fields.append('"').append(stringValue).append('"');
        } else {
            fields.append(stringValue);
        }
    }

    /**
     * This method adds the value of a field to this builder, without prefixing it with a field name.
     *
     * @param value The field value to be added to this builder.
     * @return Reference to this builder for method-chaining.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(Object value) {
        return this.append(null, value);
    }

    /**
     * Implementation of {@link Appendable#append(CharSequence)}, delegated to {@link #append(Object)}
     * to add a field value without prefixing it with a field name.
     *
     * @param value The field value to be added to this builder.
     * @return Reference to this builder for method-chaining.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(CharSequence value) {
        return this.append((Object) value);
    }

    /**
     * Implementation of {@link Appendable#append(CharSequence, int, int)} to append a partial value to this builder
     * without prefixing it with a field name.
     *
     * @param csq   The sequence to be partially appended to this builder.
     * @param start The start index, inclusive.
     * @param end   The end index, exclusive.
     * @return Reference to this builder for method-chaining.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(CharSequence csq, int start, int end) {
        return this.append(csq.subSequence(start, end));
    }

    /**
     * Implementation of {@link Appendable#append(char)} to append a single character as a field value to this builder.
     *
     * @param c The character to be appended as a field value in this builder.
     * @return Reference to this builder for method-chaining.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(char c) {
        return this.append((Character) c);
    }

    /**
     * Implementation of {@link CharSequence#subSequence(int, int)} that returns a subsequence of the current state of
     * this builder.
     *
     * @param start The start index, inclusive.
     * @param end   The end index, exclusive.
     * @return The requested subsequence for the current state of this builder.
     */
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    /**
     * This method returns whether 'content' must be rendered (between the brackets).
     *
     * @return Whether there should be content rendered between the brackets.
     */
    protected boolean hasContent() {
        return forceBrackets || fields.length() > 0;
    }

    /**
     * @return The String representation of the current state of this builder.
     */
    @Override
    public String toString() {
        return hasContent()
                ? prefix + leftBracket + fields + rightBracket
                : prefix;
    }

    /**
     * @return The current length of this builder.
     */
    public int length() {
        return hasContent()
                ? prefix.length() + leftBracket.length() + fields.length() + rightBracket.length()
                : prefix.length();
    }

    /**
     * Implementation of {@link CharSequence#charAt(int)} that returns a single character from the current state of this
     * builder.
     *
     * @param index The index of the requested character.
     * @return The character that is on position <code>index</code> in the current state of this builder.
     */
    public char charAt(int index) {
        if (index < prefix.length() || !hasContent()) return prefix.charAt(index);
        else if (index >= prefix.length() + leftBracket.length() + fields.length() + rightBracket.length()) {
            throw new StringIndexOutOfBoundsException(index);
        }
        index -= prefix.length();

        if (index < leftBracket.length()) return leftBracket.charAt(index);
        index -= leftBracket.length();

        return index < fields.length() ? fields.charAt(index) : rightBracket.charAt(index - fields.length());
    }

    /**
     * Returns the 'standard' <code>Object.toString()</code> representation as it would be returned by Object itself.
     * We require this to know when we need to reflect an appended property value or when that should not be done
     * (i.e. a custom toString() implementation was provided).
     *
     * @param value The value to return the 'standard' <code>Object.toString()</code> representatie for.
     * @return The standard toString representation for the given value.
     */
    private static String defaultToString(Object value) {
        return value == null ? null : value.getClass().getName() + "@" + Integer.toHexString(value.hashCode());
    }

    private static Class<?> classOf(Object value) {
        return value == null || value instanceof Class<?> ? (Class<?>) value : value.getClass();
    }

    /**
     * Utility method to convert objects to Strings
     * while converting <code>null</code> to the empty string (<code>""</code>).
     *
     * @param value The value to be converted to String.
     * @return The converted String value.
     */
    private static String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Utility <code>null</code>-safe equals implementation for random objects.
     *
     * @param obj1 The first object to be compared.
     * @param obj2 The second object to be compared.
     * @return <code>true</code> if the objects are equal, otherwise <code>false</code>.
     */
    private static boolean equals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    /**
     * Internal class to provide a <code>toString()</code> implementation where the result is the string between the
     * first left bracket and the last right bracket (if found).
     */
    private static final class ToStringBetween {
        private final CharSequence source;
        private final String left, right;

        private ToStringBetween(CharSequence source, String left, String right) {
            this.source = source == null ? "" : source;
            this.left = left;
            this.right = right;
        }

        /**
         * @return The 'fields' part of the super ToStringBuilder.
         */
        public String toString() {
            if (source instanceof ToStringBuilder) return ((ToStringBuilder) source).fields.toString();
            // Search for fields between first left bracket and last right bracket.
            String result = source.toString();
            final int leftIdx = result.indexOf(left);
            if (leftIdx >= 0) {
                final int start = leftIdx + left.length();
                final int end = result.lastIndexOf(right);
                result = start <= end ? result.substring(start, end) : result.substring(start);
            }
            return result;
        }
    }

    /**
     * Escape any backslash {@code '\'} and double-quote {@code '"'} characters in the given String by prefixing an
     * additional
     *
     * @param value The string value to be escaped
     * @return The resulting string with all backslash and double-quote characters escaped.
     */
    private static String escapeQuotesInString(String value) {
        final int length = value.length();
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '\"') {
                StringBuilder copy = new StringBuilder(length + length / 2).append(value.substring(0, i));
                while (i < length) {
                    ch = value.charAt(i++);
                    if (ch == '\\' || ch == '\"') copy.append('\\');
                    copy.append(ch);
                }
                return copy.toString();
            }
        }
        return value;
    }

}
