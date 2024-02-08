package dev.sunbirdrc.config;

import dev.sunbirdrc.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerConfig.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> map = new HashMap<>();

        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        Optional<String> optionalError = errors.stream().findFirst();

        if (optionalError.isPresent()) {
            map.put("code", "RC_UM_400");
            map.put("error", optionalError.get());
        } else {
            map.put("code", "RC_UM_400");
            map.put("error", exception.getMessage());
        }
        return map;
    }

//    @ExceptionHandler(UserNotFoundException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(UserNotFoundException exception) {
//        List<String> errors = Collections.singletonList(exception.getMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.NOT_FOUND);
//    }
//
//    @ExceptionHandler(OtpException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(OtpException exception) {
//        List<String> errors = Collections.singletonList(exception.getLocalizedMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(KeycloakUserException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(KeycloakUserException exception) {
//        List<String> errors = Collections.singletonList(exception.getMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(AuthorizationException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(AuthorizationException exception) {
//        List<String> errors = Collections.singletonList(exception.getMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(InvalidInputDataException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(InvalidInputDataException exception) {
//        List<String> errors = Collections.singletonList(exception.getMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }
//    @ExceptionHandler(RoleNotFoundException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(RoleNotFoundException exception) {
//        List<String> errors = Collections.singletonList(exception.getMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(UserConflictException.class)
//    public ResponseEntity<Map<String, List<String>>> handleNotFoundException(UserConflictException exception) {
//        List<String> errors = Collections.singletonList(exception.getMessage());
//        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }

    @ExceptionHandler(CustomException.class)
    public Map<String, String> handleCustomException(CustomException exception) {
        Map<String, String> map = new HashMap<>();
        map.put("code", "RC_UM_400");
        map.put("error", exception.getMessage());
        return map;
    }

    private Map<String, List<String>> getErrorsMap(List<String> errors) {
        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", errors);
        return errorResponse;
    }

}