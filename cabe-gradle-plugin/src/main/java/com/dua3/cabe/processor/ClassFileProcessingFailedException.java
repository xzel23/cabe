package com.dua3.cabe.processor;

/**
 * This exception class is thrown when processing of a class file fails.
 */
public class ClassFileProcessingFailedException extends Exception {
    /**
     * This exception class is thrown when processing of a class file fails.
     */
    public ClassFileProcessingFailedException() {
    }

    /**
     * Exception thrown when processing of a class file fails.
     *
     * @param message the exception message
     */
    public ClassFileProcessingFailedException(String message) {
        super(message);
    }

    /**
     * Represents an exception that is thrown when processing of a class file fails.
     *
     * @param message the exception message
     * @param cause the cause of the exception
     */
    public ClassFileProcessingFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * This exception is thrown when processing of a class file fails.
     *
     * @param cause the cause of the exception
     */
    public ClassFileProcessingFailedException(Throwable cause) {
        super(cause);
    }
}
