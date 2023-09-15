package com.example.validate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.model.OrderShipping;

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

	// public List<String> validate(List<OrderShipping> orderShippingList) {
	// List<String> validationError = new ArrayList<>();

	// // orderIdのバリデーション
	// if (orderShipping.getOrderId() == null || orderShipping.getOrderId() <= 0) {
	// validationResult.addError("orderId", "Invalid orderId");
	// }

	// // 型のバリデーション
	// String type = orderShipping.getShippingCode();
	// if (type == null || (!type.equals("A") && !type.equals("B") &&
	// !type.equals("C"))) {
	// validationResult.addError("shippingCode", "Invalid shippingCode");
	// }

	// return validationResult;

	// for (OrderShipping orderShipping : orderShippingList) {
	// if (!isValidOrderShipping(orderShipping)) {
	// validationError.add("エラー"); // バリデーションエラーがある場合、エラーを返す
	// }
	// validationError.add("取込完了")
	// }
	// return validationError; // すべてのオブジェクトがバリデーションをパスした場合、trueを返す
	// }

	// private boolean isValidOrderShipping(OrderShipping orderShipping) {
	// // orderIdのバリデーション
	// if (orderShipping.getOrderId() == null || orderShipping.getOrderId() <= 0) {
	// return false;
	// }

	// // 型のバリデーション
	// String shippingCode = orderShipping.getShippingCode();
	// if (shippingCode == null
	// || (!shippingCode.equals("A") && !shippingCode.equals("B") &&
	// !shippingCode.equals("C"))) {
	// return false;
	// }

	// // その他のフィールドのバリデーションロジックを追加

	// return true; // すべてのバリデーションをパスした場合、trueを返す
	// }

}