
package com.arul.authforge.service;

public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected. Please authenticate again.");
    }
}
