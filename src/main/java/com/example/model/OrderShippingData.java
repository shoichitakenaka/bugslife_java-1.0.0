package com.example.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import groovyjarjarantlr4.v4.parse.ANTLRParser.parserRule_return;

@Component
public class OrderShippingData {

	private List<OrderShipping> orderShippingList = new ArrayList<OrderShipping>();

	public List<OrderShipping> getOrderShippingList() {
		return orderShippingList;
	}

	public void setOrderShipping(List<OrderShipping> orderShipping) {
		this.orderShippingList = orderShipping;
	}

}