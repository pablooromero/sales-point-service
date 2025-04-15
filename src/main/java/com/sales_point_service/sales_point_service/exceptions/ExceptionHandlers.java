package com.sales_point_service.sales_point_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class ExceptionHandlers {
    @ExceptionHandler(SalePointException.class)
    public ResponseEntity<String> handleSalePointException(SalePointException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CostException.class)
    public ResponseEntity<String> handleCostException(CostException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
