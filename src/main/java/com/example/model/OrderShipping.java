package com.example.model;

import java.time.LocalDate;

public class OrderShipping {
	private Integer orderId;
	private String shippingCode;
	private LocalDate shippingDate;
	private LocalDate deliveryDate;
	private String deliveryTimeZone;
	private boolean checked;
	private String uploadStatus;

	// コンストラクタ
	public OrderShipping(Integer orderId, String shippingCode, LocalDate shippingDate, LocalDate deliveryDate,
			String deliveryTimeZone, String uploadStatus, boolean checked) {
		this.orderId = orderId;
		this.shippingCode = shippingCode;
		this.shippingDate = shippingDate;
		this.deliveryDate = deliveryDate;
		this.deliveryTimeZone = deliveryTimeZone;
		this.uploadStatus = uploadStatus;
		this.checked = true;
	}

	// ゲッターとセッター
	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public String getShippingCode() {
		return shippingCode;
	}

	public void setShippingCode(String shippingCode) {
		this.shippingCode = shippingCode;
	}

	public LocalDate getShippingDate() {
		return shippingDate;
	}

	public void setShippingDate(LocalDate shippingDate) {
		this.shippingDate = shippingDate;
	}

	public LocalDate getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public String getDeliveryTimeZone() {
		return deliveryTimeZone;
	}

	public void setDeliveryTimeZone(String deliveryTimeZone) {
		this.deliveryTimeZone = deliveryTimeZone;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public String getUploadStatus() {
		return uploadStatus;
	}

	public void setUploadStatus(String uploadStatus) {
		this.uploadStatus = uploadStatus;
	}
}
