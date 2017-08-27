package de.tum.msrg.itt;

/**
 * Created by pxsalehi on 30.06.16.
 */
public class IttException extends Exception {
    public IttException() {}

    public IttException(String message) {
        super(message);
    }

    public IttException(Throwable cause) {
        super(cause);
    }

    public IttException(String message, Throwable cause) {
        super(message, cause);
    }
}
