package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserConflictException extends CustomException {

    public UserConflictException(String message) {
        super(message);
    }
}
