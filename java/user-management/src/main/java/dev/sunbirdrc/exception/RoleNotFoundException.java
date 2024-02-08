package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RoleNotFoundException extends CustomException {

    public RoleNotFoundException(String message) {
        super(message);
    }
}
