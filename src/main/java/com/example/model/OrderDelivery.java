package com.example.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_delivery")
public class OrderDelivery {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "shipping_code", nullable = false)
	private String shippingCode;

	@Column(name = "shipping_date", nullable = false)
	private LocalDate shippingDate;

	@Column(name = "delivery_date", nullable = false)
	private LocalDate deliveryDate;

	@Column(name = "delivery_time_zone", nullable = false)
	private String deliveryTimezone;

	public OrderDelivery() {}

	public OrderDelivery(String shippingCode, LocalDate shippingDate, LocalDate deliveryDate,
			String deliveryTimezone) {
		this.shippingCode = shippingCode;
		this.shippingDate = shippingDate;
		this.deliveryDate = deliveryDate;
		this.deliveryTimezone = deliveryTimezone;
	}

	// ゲッターとセッターを追加
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getDeliveryTimezone() {
		return deliveryTimezone;
	}

	public void setDeliveryTimeZone(String deliveryTimezone) {
		this.deliveryTimezone = deliveryTimezone;
	}
}