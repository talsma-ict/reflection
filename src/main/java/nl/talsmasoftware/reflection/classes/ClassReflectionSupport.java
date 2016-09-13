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
public final class ClassReflectionSupport {
    private static final Logger LOGGER = Logger.getLogger(ClassReflectionSupport.class.getName());
    private static final Object[] NO_PARAMS = new Object[0];
    private static final Class[] NO_TYPES = new Class[0];

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ClassReflectionSupport() {
        throw new UnsupportedOperationException();
    }

    public static Class<?> findClass(String className) {
        try {

            // TODO: Apply some general (defensive but correct) classloader strategy!
            return Class.forName(className);

        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.FINEST,
                    "Class \"{0}\" not found. It is likely that some JAR library is not available on the classpath.",
                    className);
        } catch (LinkageError e) {
            LOGGER.log(Level.FINEST, "Linkage error while loading \"{0}\" class. " +
                            "It is likely that there is an incompatible version of some JAR library on the classpath.",
                    new Object[]{className, e});
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINEST, "Unexpected exception attempting to load class \"{0}\": {1}",
                    new Object[]{className, e.getMessage(), e});
        }
        return null;
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

}
