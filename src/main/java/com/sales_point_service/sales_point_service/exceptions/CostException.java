package com.sales_point_service.sales_point_service.exceptions;

import org.springframework.http.HttpStatus;
public class CostException extends RuntimeException {
    private HttpStatus httpStatus;
    public CostException(String message) {
        super(message);
    }

    public CostException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {return httpStatus;}
}