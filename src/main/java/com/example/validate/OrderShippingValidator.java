package com.example.validate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class OrderShippingValidator extends RuntimeException {
	List<String> validationError = new ArrayList<>();

	public OrderShippingValidator(List<String> validationError) {
		super("Validation failed");
		this.validationError = validationError;
	}

	public List<String> getErrors() {
		return validationError;
	}
}