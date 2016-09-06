package de.otto.edison.hal;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Information about errors that occured during resource retrieval by {@link Traverson Traversons}.
 *
 * @since 0.4.0
 */
public final class TraversionError {

    /**
     * The error type of a TraversionError.
     */
    public enum Type {
        /**
         * Indicates that the specified resource was not found.
         */
        NOT_FOUND,
        /**
         * Indicates, that the error occured because of an invalid JSON format.
         */
        INVALID_JSON,
        /**
         * Indicates, that one of the specified link-relation types was not found in a resource.
         */
        MISSING_LINK;

    }

    private final Type type;
    private final String message;
    private final Exception cause;

    private TraversionError(final Type type, final String message, final Exception cause) {
        if (type == null) {
            throw new NullPointerException("Parameter 'type' must not be null");
        }
        if (message == null) {
            throw new NullPointerException("Parameter 'message' must not be null");
        }
        this.type = type;
        this.message = message;
        this.cause = cause;
    }

    public static <T> TraversionError traversionError(final Type type, final String message, final Exception cause) {
        return new TraversionError(type, message, cause);
    }

    public static <T> TraversionError traversionError(final Type type, final String message) {
        return new TraversionError(type, message, null);
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Optional<Exception> getCause() {
        return ofNullable(cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraversionError that = (TraversionError) o;

        if (type != that.type) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return cause != null ? cause.equals(that.cause) : that.cause == null;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (cause != null ? cause.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TraversionError{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", cause=" + cause +
                '}';
    }
}
