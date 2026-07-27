package com.featureforge.exception;

/**
 * Thrown by ApiKeyService when a raw SDK key can't be resolved to a project
 * (missing, unknown prefix, revoked, or hash mismatch). Caught directly inside
 * ApiKeyAuthFilter — it never reaches GlobalExceptionHandler because filters
 * run before the DispatcherServlet.
 */
public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException(String message) {
        super(message);
    }
}
