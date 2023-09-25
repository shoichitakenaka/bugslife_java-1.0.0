package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.model.OrderDelivery;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, Long> {

}
