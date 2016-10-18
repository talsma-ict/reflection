package nl.talsmasoftware.reflection.errorhandling;

/**
 * @author Sjoerd Talsma
 */
public class InstantiatingException extends MethodInvocationException {

    public InstantiatingException(String message, Throwable cause) {
        super(message, cause);
    }

}
