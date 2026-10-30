
package com.onlineexam.backend.exception;

public class EmailAlreadyRegisteredException
        extends RuntimeException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
