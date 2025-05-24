package org.traccar.geoconv;

public class GeoConvException extends Exception {

    public GeoConvException(String message) {
        super(message);
    }

    public GeoConvException(Throwable cause) {
        super(cause);
    }

    public GeoConvException(String message, Throwable cause) {
        super(message, cause);
    }
}
