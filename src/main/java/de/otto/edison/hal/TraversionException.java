package de.otto.edison.hal;

class TraversionException extends RuntimeException {

    private final TraversionError error;

    TraversionException(final TraversionError error) {
        super(error.getMessage());
        this.error = error;
    }

    TraversionError getError() {
        return error;
    }
}
