package com.ledgeflow.wallet.exception;

public class KycLimitExceededException extends RuntimeException {
    public KycLimitExceededException(String message) {
        super(message);
    }
}
