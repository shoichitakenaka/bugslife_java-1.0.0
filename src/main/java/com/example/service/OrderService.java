package com.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.constants.TaxType;
import com.example.enums.OrderStatus;
import com.example.enums.PaymentStatus;
import com.example.form.OrderForm;
import com.example.model.Order;
import com.example.model.OrderDelivery;
import com.example.model.OrderPayment;
import com.example.model.OrderProduct;
import com.example.model.OrderShipping;
import com.example.model.OrderShippingData;
import com.example.repository.OrderDeliveryRepository;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import com.example.validate.OrderShippingValidator;

@Service
@Transactional(readOnly = true)
public class OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderDeliveryRepository orderDeliveryRepository;

	@Autowired
	private ProductRepository productRepository;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public OrderService(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = false)
	public OrderDelivery save(OrderDelivery entity) {
		return orderDeliveryRepository.save(entity);
	}

	public List<Order> findAll() {
		return orderRepository.findAll();
	}

	public Optional<Order> findOne(Long id) {
		return orderRepository.findById(id);
	}

	@Transactional(readOnly = false)
	public Order save(Order entity) {
		return orderRepository.save(entity);
	}

	@Transactional(readOnly = false)
	public Order create(OrderForm.Create entity) {
		Order order = new Order();
		order.setCustomerId(entity.getCustomerId());
		order.setShipping(entity.getShipping());
		order.setNote(entity.getNote());
		order.setPaymentMethod(entity.getPaymentMethod());
		order.setStatus(OrderStatus.ORDERED);
		order.setPaymentStatus(PaymentStatus.UNPAID);
		order.setPaid(0.0);

		var orderProducts = new ArrayList<OrderProduct>();
		entity.getOrderProducts().forEach(p -> {
			var product = productRepository.findById(p.getProductId()).get();
			var orderProduct = new OrderProduct();
			orderProduct.setProductId(product.getId());
			orderProduct.setCode(product.getCode());
			orderProduct.setName(product.getName());
			orderProduct.setQuantity(p.getQuantity());
			orderProduct.setPrice((double)product.getPrice());
			orderProduct.setDiscount(p.getDiscount());
			orderProduct.setTaxType(TaxType.get(product.getTaxType()));
			orderProducts.add(orderProduct);
		});

		// 計算
		var total = 0.0;
		var totalTax = 0.0;
		var totalDiscount = 0.0;
		for (var orderProduct : orderProducts) {
			var price = orderProduct.getPrice();
			var quantity = orderProduct.getQuantity();
			var discount = orderProduct.getDiscount();
			var tax = 0.0;
			/**
			 * 税額を計算する
			 */
			if (orderProduct.getTaxIncluded()) {
				// 税込みの場合
				tax = price * quantity * orderProduct.getTaxRate() / (100 + orderProduct.getTaxRate());
			} else {
				// 税抜きの場合
				tax = price * quantity * orderProduct.getTaxRate() / 100;
			}
			// 端数処理
			tax = switch (orderProduct.getTaxRounding()) {
			case TaxType.ROUND -> Math.round(tax);
			case TaxType.CEIL -> Math.ceil(tax);
			case TaxType.FLOOR -> Math.floor(tax);
			default -> tax;
			};
			var subTotal = price * quantity + tax - discount;
			total += subTotal;
			totalTax += tax;
			totalDiscount += discount;
		}
		order.setTotal(total);
		order.setTax(totalTax);
		order.setDiscount(totalDiscount);
		order.setGrandTotal(total + order.getShipping());
		order.setOrderProducts(orderProducts);

		orderRepository.save(order);

		return order;

	}

	@Transactional()
	public void delete(Order entity) {
		orderRepository.delete(entity);
	}

	@Transactional(readOnly = false)
	public void createPayment(OrderForm.CreatePayment entity) {
		var order = orderRepository.findById(entity.getOrderId()).get();
		/**
		 * 新しい支払い情報を登録する
		 */
		var payment = new OrderPayment();
		payment.setType(entity.getType());
		payment.setPaid(entity.getPaid());
		payment.setMethod(entity.getMethod());
		payment.setPaidAt(entity.getPaidAt());

		/**
		 * 支払い情報を更新する
		 */
		// orderのorderPaymentsに追加
		order.getOrderPayments().add(payment);
		// 支払い済み金額を計算
		var paid = order.getOrderPayments().stream().mapToDouble(p -> p.getPaid()).sum();
		// 合計金額から支払いステータスを判定
		var paymentStatus = paid > order.getGrandTotal() ? PaymentStatus.OVERPAID
				: paid < order.getGrandTotal() ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID;

		// 更新
		order.setPaid(paid);
		order.setPaymentStatus(paymentStatus);
		orderRepository.save(order);
	}

	/**
	 * CSVインポート処理
	 *
	 * @param file
	 * @throws IOException
	 */
	@Transactional
	public List<OrderShipping> importCSV(MultipartFile file) throws IOException, NumberFormatException {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
			List<OrderShipping> orderShippingList = new ArrayList<>();
			List<String> validationError = new ArrayList<>();
			System.out.println("CSVファイルのインポートが完了。");

			int csvLine = 1;
			while ((line = br.readLine()) != null) {
				try {
					final String[] split = line.replace("\"", "").split(",");
					final OrderShipping orderShipping = new OrderShipping(Integer.parseInt(split[0]), split[1],
							LocalDate.parse(split[2]), LocalDate.parse(split[3]), split[4], null, true);
					orderShippingList.add(orderShipping);

				} catch (NumberFormatException | DateTimeParseException e) {
					validationError.add(csvLine + "行目が不正です");
					// throw new OrderShippingValidator(validationError);
				}
				csvLine++;
			}
			int count = 1;
			for (OrderShipping orderShipping : orderShippingList) {
				// ここにvalidationする処理を書く
				if (orderShipping.getOrderId() == null || !(orderShipping.getOrderId() instanceof Integer)) {
					validationError.add(count + "行目のorderIdが不正です。");
				}
				if (orderShipping.getShippingCode() == null
						|| !(orderShipping.getShippingCode() instanceof String)) {
					validationError.add(count + "行目shippingCodeが不正です。");
				}
				if (orderShipping.getShippingDate() == null
						|| !(orderShipping.getShippingDate() instanceof LocalDate)) {
					validationError.add(count + "行目shippingDateが不正です。");
				}
				if (orderShipping.getDeliveryDate() == null
						|| !(orderShipping.getDeliveryDate() instanceof LocalDate)) {
					validationError.add(count + "行目deliveryDateが不正です。");
				}
				if (orderShipping.getDeliveryTimeZone() == null
						|| !(orderShipping.getDeliveryTimeZone() instanceof String)) {
					validationError.add(count + "行目deliveryTimeZoneが不正です。");
				}
				count++;
			}
			if (!validationError.isEmpty()) {
				throw new OrderShippingValidator(validationError);
			}
			return orderShippingList;
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
	}

	/**
	 * 一括更新処理実行
	 *
	 * @param orderDeliveries
	 */
	public OrderShippingData OrderSave(OrderShippingData orderShippingData) throws Exception {
		// String sql = "INSERT INTO order_delivery (shipping_code, shipping_date,
		// delivery_date, delivery_time_zone)"
		// + " VALUES(:shipping_code, :shipping_date, :delivery_date,
		// :delivery_time_zone)";
		// return jdbcTemplate.batchUpdate(sql,
		// orderDeliveries.stream()
		// .map(o -> new MapSqlParameterSource()
		// .addValue("shipping_code", o.getShippingCode(), Types.VARCHAR)
		// .addValue("shipping_date", o.getShippingDate(), Types.DATE)
		// .addValue("delivery_date", o.getDeliveryDate(), Types.DATE)
		// .addValue("delivery_time_zone", o.getDeliveryTimezone(), Types.VARCHAR))
		// .toArray(SqlParameterSource[]::new));
		int count = 0;
		List<OrderShipping> orderShippingList = orderShippingData.getOrderShippingList();
		try {
			for (OrderShipping orderShipping : orderShippingList) {
				System.out.println("CSVファイルのインポートが完了。");
				Order order = new Order();
				order.setPaymentStatus("発送完了");
				order.setId((long)orderShipping.getOrderId());
				orderRepository.save(order);
				OrderDelivery orderDelivery = new OrderDelivery();
				orderDelivery.setId((long)orderShipping.getOrderId());
				orderDelivery.setShippingCode(orderShipping.getShippingCode());
				orderDelivery.setShippingDate(orderShipping.getShippingDate());
				orderDelivery.setDeliveryDate(orderShipping.getDeliveryDate());
				orderDelivery.setDeliveryTimeZone(orderShipping.getDeliveryTimeZone());
				orderDeliveryRepository.save(orderDelivery);
				orderShippingList.get(count).setUploadStatus("success");
				count++;
			}
			orderShippingData.setOrderShipping(orderShippingList);
		} catch (Exception e) {
			// TODO: handle exception
			orderShippingList.get(count).setUploadStatus("error");
		}
		return orderShippingData;
	}

	// /**
	// * 一括ステータス更新処理 更新前にステータスをチェックする
	// *
	// * @param idList 更新対象IDリスト
	// * @param nexStatus 更新後ステータス
	// * @throws Exception
	// */
	// @Transactional(readOnly = false, rollbackFor = RuntimeException.class)
	// public void bulkStatusUpdate(List<Long> idList, OrderStatus nexStatus) throws
	// Exception {
	// try {

	// for (Long id : idList) {
	// Campaign campaign = campaignRepository.findById(id).get();
	// OrderShipping.setStatus(nexStatus);
	// orderRepository.save(campaign);
	// }
	// } catch (RuntimeException e) {
	// throw new Exception(e.getMessage());
	// }
	// }

}
