package com.crewmeister.cmcodingchallenge.exception;

import com.crewmeister.cmcodingchallenge.currency.RateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Handles request parameter and path-variable type mismatches.
     *
     * @param e Spring type mismatch exception
     * @return fixed bad-request message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return "Invalid input";
    }

    /**
     * Handles missing rate data errors.
     *
     * @param e missing-rate exception
     * @return not-found message
     */
    @ExceptionHandler(RateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(RateNotFoundException e) {
        // No rate exists for that date
        return e.getMessage();
    }

    /**
     * Handles invalid request input.
     *
     * @param e validation exception
     * @return bad-request message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException e) {
        return e.getMessage();
    }
}
