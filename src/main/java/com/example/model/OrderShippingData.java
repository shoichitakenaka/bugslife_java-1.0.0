package com.example.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

// htmlへデータを渡すためのクラス
@Component
public class OrderShippingData {

	private List<OrderShipping> orderShippingList = new ArrayList<OrderShipping>();

	// ゲッターとセッター
	public List<OrderShipping> getOrderShippingList() {
		return orderShippingList;
	}

	public void setOrderShipping(List<OrderShipping> orderShipping) {
		this.orderShippingList = orderShipping;
	}

}
