package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AuthorizationException extends CustomException {

    public AuthorizationException(String message) {
        super(message);
    }
}
