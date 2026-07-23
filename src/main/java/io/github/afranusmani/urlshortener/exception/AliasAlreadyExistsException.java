package io.github.afranusmani.urlshortener.exception;

/**
 * Thrown when a requested custom alias is already in use. Mapped to HTTP 409 Conflict.
 */
public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(String alias) {
        super("Short code already in use: " + alias);
    }
}
