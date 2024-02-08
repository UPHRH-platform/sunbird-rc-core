package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CustomException extends RuntimeException {

    public CustomException(String message) {
        super(message);
    }
}
