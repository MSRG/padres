package de.tum.msrg.itt;

/**
 * Created by pxsalehi on 30.06.16.
 */
public class IttSocketTimeOutException extends IttException {
    public IttSocketTimeOutException() {}

    public IttSocketTimeOutException(String message) {
        super(message);
    }

    public IttSocketTimeOutException(Throwable cause) {
        super(cause);
    }

    public IttSocketTimeOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
