package de.tum.msrg.itt;

/**
 * Created by pxsalehi on 30.06.16.
 */
public class IttWrongOperandException extends IttException {
    public IttWrongOperandException() {}

    public IttWrongOperandException(String message) {
        super(message);
    }

    public IttWrongOperandException(Throwable cause) {
        super(cause);
    }

    public IttWrongOperandException(String message, Throwable cause) {
        super(message, cause);
    }
}
