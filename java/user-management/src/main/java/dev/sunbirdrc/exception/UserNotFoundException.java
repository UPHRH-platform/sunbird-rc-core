package dev.sunbirdrc.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserNotFoundException extends CustomException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
