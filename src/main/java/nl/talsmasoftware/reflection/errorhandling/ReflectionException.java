package nl.talsmasoftware.reflection.errorhandling;

/**
 * @author Sjoerd Talsma
 */
public abstract class ReflectionException extends RuntimeException {

    protected ReflectionException(String message, Throwable cause) {
        super(message);
        if (cause != null) super.initCause(cause);
    }

}
