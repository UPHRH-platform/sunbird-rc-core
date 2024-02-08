package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OtpException extends CustomException {

    public OtpException(String message) {
        super(message);
    }
}
