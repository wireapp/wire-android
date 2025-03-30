package com.wearezeta.auto.common.usrmgmt;

public class UserAlreadyCreatedException extends RuntimeException {

    public UserAlreadyCreatedException(String message) {
        super(message);
    }

}
