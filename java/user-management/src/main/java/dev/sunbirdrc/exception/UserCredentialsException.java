package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserCredentialsException extends CustomException{
    public UserCredentialsException(String message) {
        super(message);
    }
}
