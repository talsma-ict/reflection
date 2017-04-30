/*
 * Copyright (C) 2017 Talsma ICT
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

package nl.talsmasoftware.test;

import junit.framework.AssertionFailedError;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class with methods intented for (unit-)testing.
 * <p>
 * For example there are reflective operations that allow access to private fields,
 * methods or constructors which is normally a bad idea for production code,
 * but in some situations can be useful for unit testing.
 *
 * @author Sjoerd Talsma.
 */
public final class TestUtil {
    private static final Logger LOGGER = Logger.getLogger(TestUtil.class.getName());
    private static final Random RND = new SecureRandom();
    private static final String DEFAULT_RANDOM_CHARS =
            " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

    private TestUtil() {
        throw new UnsupportedOperationException("Utility class may not be instantiated.");
    }

    /**
     * Simulates field-injection in test situations.
     * <p>
     * This method can be used for setting both for object fields and static fields.
     * For static fields, the {@link Class} should be specified as the 'object' argument.
     *
     * @param object    The object to set the field for (or Class in case of static fields).
     * @param fieldName The name of the field to be set.
     * @param value     The new value for the field.
     */
    public static void setPrivateField(Object object, String fieldName, Object value) {
        Class<?> type = object.getClass();
        if (object instanceof Class<?>) { // Static aanroep
            type = (Class<?>) object;
            object = null;
        }
        Field field = findField(type, fieldName);
        synchronized (field) {
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(object, value);
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException(iae.getMessage(), iae);
            } finally {
                field.setAccessible(accessible);
            }
        }
    }

    /**
     * Method to test results that are only accessible in private fields.
     * <p>
     * This method can be used for getting both for object fields and static fields.
     * For static fields, the {@link Class} should be specified as the 'object' argument.
     *
     * @param object    The object to get the private field from or the Class for a static field.
     * @param fieldName The name of the field to get.
     * @param <T>       The type of the requested field.
     * @return The value from the field.
     */
    public static <T> T getPrivateField(Object object, String fieldName) {
        Class<?> type = object.getClass();
        if (object instanceof Class<?>) { // Static aanroep
            type = (Class<?>) object;
            object = null;
        }
        Field field = findField(type, fieldName);
        synchronized (field) {
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                return (T) field.get(object);
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException(iae.getMessage(), iae);
            } finally {
                field.setAccessible(accessible);
            }
        }
    }

    /**
     * Common reflection-code for getPrivateField and setPrivateField methods.
     */
    private static Field findField(Class<?> searchType, String name) {
        Field field = null;
        Class<?> type = searchType;
        do {
            try {
                field = type.getDeclaredField(name);
            } catch (NoSuchFieldException nietGevonden) {
                LOGGER.log(Level.FINEST, "Field \"{0}\" not found in {1}. Let's try {2}...",
                        new Object[]{name, type, type.getSuperclass()});
                type = type.getSuperclass();
            }
        } while (field == null && type != null);
        if (field == null) {
            throw new IllegalStateException(String.format("Field \"%s\" not found in %s or any of its superclasses.",
                    name, searchType));
        }
        return field;
    }

    /**
     * This method creates a new random {@code String} of a maximum of '{@code maxlength}' characters.
     * <p>
     * The random characters are taken from the 'readable part' of the lower ascii range: {@code ' '} until {@code '~'}.
     * <p>
     * The length of the returned String is also random and will be between zero ({@code 0}) and {@code maxlength},
     * both inclusive.
     * For strings with a minimum length or a certain character-choice,
     * please see {@link #randomString(int, int, CharSequence)}.
     *
     * @param maxlength The maximum number of characters the random string may contain (&gt;= 0).
     * @return A new random string.
     * @see #randomString(int, int)
     * @see #randomString(int, int, CharSequence)
     * @see #randomStrings(int, int, int)
     * @see #randomStrings(int, int, int, CharSequence)
     */
    public static String randomString(int maxlength) {
        return randomString(0, maxlength, null);
    }

    /**
     * This method creates a new random {@code String}.
     * <p>
     * The length of the returned string is random and will be between {@code minlength} and {@code maxlength},
     * both inclusive.
     * <p>
     * The random characters are taken from the 'readable part' of the lower ascii range: {@code ' '} until {@code '~'}.
     * For strings with a certain character-choice, please see {@link #randomString(int, int, CharSequence)}.
     *
     * @param minlength The minimum number of characters the random string may contain (&gt;= 0).
     * @param maxlength The maximum number of characters the random string may contain (&gt;= minlength).
     * @return A new random string.
     * @see #randomString(int)
     * @see #randomString(int, int, CharSequence)
     * @see #randomStrings(int, int, int)
     * @see #randomStrings(int, int, int, CharSequence)
     */
    public static String randomString(int minlength, int maxlength) {
        return randomString(minlength, maxlength, null);
    }

    /**
     * This method creates a new random {@code String}.
     * <p>
     * The length of the returned string is random and will be between {@code minlength} and {@code maxlength},
     * both inclusive.
     * <p>
     * The random characters are taken from the specified '{@code randomChars}' {@link CharSequence}.
     * If {@code randomChars} is {@code null}, the characters will be chosen from the 'readable part'
     * of the lower ascii range: {@code ' '} until {@code '~'}.
     *
     * @param minlength   The minimum number of characters the random string may contain (&gt;= 0).
     * @param maxlength   The maximum number of characters the random string may contain (&gt;= minlength).
     * @param randomChars The characters the randomizer may choose from or {@code null} for 'readable' ascii characters.
     * @return A new random string.
     * @see #randomString(int)
     * @see #randomString(int, int)
     * @see #randomStrings(int, int, int)
     * @see #randomStrings(int, int, int, CharSequence)
     */
    public static String randomString(int minlength, int maxlength, CharSequence randomChars) {
        checkArgument(minlength >= 0, "Minimum length may not be negative.");
        checkArgument(maxlength >= minlength, "Maximum length may not be less than minimum length.");
        char[] chars = new char[minlength + RND.nextInt(maxlength - minlength + 1)];
        CharSequence source = randomChars == null ? DEFAULT_RANDOM_CHARS : randomChars;
        for (int i = 0; i < chars.length; i++) {
            chars[i] = source.charAt(RND.nextInt(source.length()));
        }
        return new String(chars);
    }

    /**
     * This method creates a new array of '{@code arraySize}' random strings.
     * <p>
     * The length of each string is random and will be between {@code minlength} and {@code maxlength}, both inclusive.
     * <p>
     * The random characters are taken from the 'readable part' of the lower ascii range: {@code ' '} until {@code '~'}.
     *
     * @param arraySize The number of random strings to be created.
     * @param minlength The minimum number of characters the random string may contain (&gt;= 0).
     * @param maxlength The maximum number of characters the random string may contain (&gt;= minlength).
     * @return An array of 'arraySize' random strings between minlength and maxlength characters.
     * @see #randomString(int, int)
     * @see #randomStrings(int, int, int, CharSequence)
     */
    public static String[] randomStrings(int arraySize, int minlength, int maxlength) {
        return randomStrings(arraySize, minlength, maxlength, null);
    }

    /**
     * This method creates a new array of '{@code arraySize}' random strings.
     * <p>
     * The length of each string is random and will be between {@code minlength} and {@code maxlength}, both inclusive.
     * <p>
     * The random characters are taken from the specified '{@code randomChars}' {@link CharSequence}.
     * If {@code randomChars} is {@code null}, the characters will be chosen from the 'readable part'
     * of the lower ascii range: {@code ' '} until {@code '~'}.
     *
     * @param arraySize   The number of random strings to be created.
     * @param minlength   The minimum number of characters the random string may contain (&gt;= 0).
     * @param maxlength   The maximum number of characters the random string may contain (&gt;= minlength).
     * @param randomChars The characters the randomizer may choose from or {@code null} for 'readable' ascii characters.
     * @return An array of 'arraySize' random strings between minlength and maxlength characters.
     * @see #randomString(int, int, CharSequence)
     * @see #randomStrings(int, int, int)
     */
    public static String[] randomStrings(int arraySize, int minlength, int maxlength, CharSequence randomChars) {
        checkArgument(arraySize >= 0, "Array size may not be negative.");
        String[] randomStrings = new String[arraySize];
        for (int i = 0; i < randomStrings.length; i++) {
            randomStrings[i] = randomString(minlength, maxlength, randomChars);
        }
        return randomStrings;
    }

    /**
     * Method to test serialization behaviour by serializing the passed object into a new byte array.
     *
     * @param value The object to be serialized.
     * @return The byte representation of the passed object.
     * @see #deserialize(byte[])
     */
    public static byte[] serialize(Serializable value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream stream = null;
        RuntimeException exception = null;
        try {
            stream = new ObjectOutputStream(bytes);
            stream.writeObject(value);
            return bytes.toByteArray();
        } catch (IOException ioe) {
            exception = new IllegalStateException("IO exception: " + ioe.getMessage(), ioe);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException ioe) {
                if (exception == null) exception = new IllegalStateException("Cannot close stream.", ioe);
            }
        }
        throw exception;
    }

    /**
     * Method to test serialization behaviour by deserializing the byte representation back into object form.
     *
     * @param value The byte representation of the serialized object.
     * @param <S>   The type of the serialized object.
     * @return The deserialized object.
     * @see #serialize(Serializable)
     */

    public static <S extends Serializable> S deserialize(byte[] value) {
        try {
            return (S) new ObjectInputStream(new ByteArrayInputStream(value)).readObject();
        } catch (IOException ioe) {
            throw new IllegalStateException("IO fout: " + ioe.getMessage(), ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException("Class not found on the classpath: " + cnfe.getMessage(), cnfe);
        }
    }

    /**
     * This method checks whether the exception contains a non-empty message.
     *
     * @param exception The exception to be checked.
     */
    public static void assertExceptionMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().trim().length() == 0) {
            AssertionFailedError assertionFailedError =
                    new AssertionFailedError("Exception doesn't contain a message: " + exception);
            assertionFailedError.initCause(exception);
            throw assertionFailedError;
        }
    }

    /**
     * This method checks whether the specified '{@code utilityClass}' is in fact a utility class.
     * This means fulfilling the following demands:
     * <p>
     * <ol>
     * <li>The utility class must be {@code final}.</li>
     * <li>The utility class must have exactly one constructor that takes no arguments.</li>
     * <li>This constructor must be private and throw an {@link UnsupportedOperationException}.</li>
     * </ol>
     *
     * @param utilityKlasse The utility class to be verified.
     */
    public static void assertUnsupportedConstructor(Class<?> utilityKlasse) {
        if (utilityKlasse == null) {
            fail("Utility class is <null>.");
        } else if (!Modifier.isFinal(utilityKlasse.getModifiers())) {
            fail("Utility class is not final: " + utilityKlasse.getName());
        } else if (utilityKlasse.getDeclaredConstructors().length != 1) {
            fail("Utility class doesn't have exactly 1 declared constructor: " + utilityKlasse.getName());
        }
        Constructor<?> constructor = utilityKlasse.getDeclaredConstructors()[0];
        if (constructor.getParameterTypes().length != 0) {
            fail("Utility class has a constructor that requires parameters: " + utilityKlasse.getName());
        } else if (!Modifier.isPrivate(constructor.getModifiers()) || constructor.isAccessible()) {
            fail("Utility class constructor is not private: " + utilityKlasse.getName());
        }
        try {
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Utility class constructor doesn't throw an UnsupportedOperationException: " + utilityKlasse.getName());
        } catch (InvocationTargetException expected) {
            Throwable cause = expected.getCause();
            if (cause == null) {
                fail("No cause for InvocationTargetException.", expected);
            }
            if (!(cause instanceof UnsupportedOperationException)) {
                fail(String.format("Utility class constructor for %s doesn't throw an " +
                                "UnsuportedOperationException but %s.",
                        utilityKlasse.getSimpleName(), cause.getClass().getName()), cause);
            }
        } catch (InstantiationException e) {
            fail(String.format("Unexpected excption from constructor of %s: %s",
                    utilityKlasse.getName(), e.getMessage()), e);
        } catch (IllegalAccessException e) {
            fail(String.format("Not allowed to test private constructor of %s: %s",
                    utilityKlasse.getName(), e.getMessage()), e);
        } finally {
            constructor.setAccessible(false);
        }
    }

    // Method to avoid external dependencies.
    static void checkArgument(boolean expression, Object message, Object... params) {
        if (!expression) {
            String errorMessage = String.valueOf(message);
            if (params.length > 0) {
                try {
                    errorMessage = String.format(errorMessage, params);
                } catch (RuntimeException formattingError) {
                    LOGGER.log(Level.FINEST, "Can't format message \"{0}\" with arguments {1}: {2}",
                            new Object[]{errorMessage, Arrays.toString(params), formattingError.getMessage(), formattingError});
                }
            }
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void fail(String message) {
        fail(message, null);
    }

    /**
     * Deze operatie laat de test falen met de opgegeven message.
     *
     * @param message De message voor de falende test (optioneel, {@code null} levert een algemene error zonder message op).
     */
    public static void fail(String message, Throwable cause) {
        AssertionError failure = message == null ? new AssertionError() : new AssertionError(message);
        if (cause != null) failure.initCause(cause);
        throw failure;
    }

    /**
     * Returns the test fixture as a String, based on UTF-8 encoding.
     *
     * @param resourceName The name of the resource to loas as a String.
     * @return The loaded fixture.
     */
    public static String fixture(String resourceName) {
        return fixture(resourceName, "UTF-8");
    }

    static String fixture(String fixture, String encoding) {
        Reader reader = null;
        RuntimeException exception = null;
        try {
            reader = new InputStreamReader(resourceAsStream(fixture), encoding);
            StringWriter writer = new StringWriter();
            char[] buf = new char[1024];
            for (int read = reader.read(buf); read >= 0; read = reader.read(buf)) {
                writer.write(buf, 0, read);
            }
            return writer.toString();
        } catch (IOException ioe) {
            exception = new IllegalStateException("I/O exception reading fixture " + fixture, ioe);
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException ioe) {
                if (exception == null) exception = new IllegalStateException("Can't close reader for " + fixture, ioe);
            }
        }
        throw exception;
    }

    /**
     * Utility method to locate test resources.
     *
     * @param resource The name of the test resource to return as InputStream.
     * @return The non-<code>null</code> InputStream to the specified resource.
     * @throws IllegalStateException when the resource can't be read.
     */
    public static InputStream resourceAsStream(String resource) {
        try {
            for (Class<?> c : new Class[]{Class.forName(findCallerClassName()), TestUtil.class}) {
                InputStream stream = c.getResourceAsStream(resource);
                if (stream != null) {
                    return stream;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException("Couldn't load calling class: " + cnfe.getMessage(), cnfe);
        }
        throw new IllegalStateException(String.format("Resource not found: '%s'.", resource));
    }

    /**
     * Internal method to find the name of the calling class.
     */
    private static String findCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackElement : stackTrace) {
            String className = stackElement.getClassName();
            if (!className.startsWith("java.lang")
                    && !className.startsWith(TestUtil.class.getPackage().getName())) {
                LOGGER.log(Level.FINEST, "Found caller classname: \"{0}\".", className);
                return className;
            }
        }
        String fallbackClassName = stackTrace[stackTrace.length - 1].getClassName();
        LOGGER.log(Level.FINE, "Caller classname could not be determined. We will return \"{0}\" as fallback.", fallbackClassName);
        return fallbackClassName;
    }
}
