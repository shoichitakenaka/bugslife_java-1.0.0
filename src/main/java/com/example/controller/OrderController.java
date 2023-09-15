package com.example.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.constants.Message;
import com.example.enums.OrderStatus;
import com.example.enums.PaymentMethod;
import com.example.enums.PaymentStatus;
import com.example.form.OrderForm;
import com.example.model.Order;
import com.example.model.OrderShipping;
import com.example.model.OrderShippingData;
import com.example.service.OrderService;
import com.example.service.ProductService;
import com.example.validate.OrderShippingValidator;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private ProductService productService;

	@Autowired
	public OrderShippingData orderShippingData;

	@GetMapping
	public String index(Model model) {
		List<Order> all = orderService.findAll();
		model.addAttribute("listOrder", all);
		return "order/index";
	}

	@GetMapping("/shipping")
	public String shipping(Model model) {
		// List<Order> all = orderService.findAll();
		// model.addAttribute("listOrder", all);
		return "order/shipping";
	}

	@GetMapping("/{id}")
	public String show(Model model, @PathVariable("id") Long id) {
		if (id != null) {
			Optional<Order> order = orderService.findOne(id);
			model.addAttribute("order", order.get());
		}
		return "order/show";
	}

	@GetMapping(value = "/new")
	public String create(Model model, @ModelAttribute OrderForm.Create entity) {
		model.addAttribute("order", entity);
		model.addAttribute("products", productService.findAll());
		model.addAttribute("paymentMethods", PaymentMethod.values());
		return "order/create";
	}

	@PostMapping
	public String create(@Validated @ModelAttribute OrderForm.Create entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		Order order = null;
		try {
			order = orderService.create(entity);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_INSERT);
			return "redirect:/orders/" + order.getId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/orders";
		}
	}

	@GetMapping("/{id}/edit")
	public String update(Model model, @PathVariable("id") Long id) {
		try {
			if (id != null) {
				Optional<Order> entity = orderService.findOne(id);
				model.addAttribute("order", entity.get());
				model.addAttribute("paymentMethods", PaymentMethod.values());
				model.addAttribute("paymentStatus", PaymentStatus.values());
				model.addAttribute("orderStatus", OrderStatus.values());
			}
		} catch (Exception e) {
			throw new ServiceException(e.getMessage());
		}
		return "order/form";
	}

	@PutMapping
	public String update(@Validated @ModelAttribute Order entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		Order order = null;
		try {
			order = orderService.save(entity);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_UPDATE);
			return "redirect:/orders/" + order.getId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/orders";
		}
	}

	@DeleteMapping("/{id}")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		try {
			if (id != null) {
				Optional<Order> entity = orderService.findOne(id);
				orderService.delete(entity.get());
				redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_DELETE);
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			throw new ServiceException(e.getMessage());
		}
		return "redirect:/orders";
	}

	@PostMapping("/{id}/payments")
	public String createPayment(@Validated @ModelAttribute OrderForm.CreatePayment entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.createPayment(entity);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_PAYMENT_INSERT);
			return "redirect:/orders/" + entity.getOrderId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/orders";
		}
	}

	/**
	 * CSVインポート処理
	 *
	 * @param uploadFile
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/shipping/upload_file")
	public String uploadFile(@RequestParam("file") MultipartFile uploadFile,
			RedirectAttributes redirectAttributes, Model model) {
		System.out.println("完了しました。");
		if (uploadFile.isEmpty()) {
			// ファイルが存在しない場合
			System.out.println("ファイルなし");
			redirectAttributes.addFlashAttribute("error", "ファイルを選択してください。");
			return "redirect:/orders/shipping";
		}
		if (!"text/csv".equals(uploadFile.getContentType())) {
			// CSVファイル以外の場合
			System.out.println("ファイル違う");
			redirectAttributes.addFlashAttribute("error", "CSVファイルを選択してください。");
			return "redirect:/orders/shipping";
		}
		try {
			System.out.println("CSVファイルのインポートが完了しました。");
			List<OrderShipping> orderShippingList = new ArrayList<OrderShipping>();
			orderShippingList = orderService.importCSV(uploadFile);
			orderShippingData.setOrderShipping(orderShippingList);
			model.addAttribute("orderShippingData", orderShippingData);
			// redirectAttributes.addFlashAttribute("orderShippingData",
			// orderService.importCSV(uploadFile));
			// System.out.println(orderService.importCSV(uploadFile));
			for (OrderShipping orderShipping : orderShippingData.getOrderShippingList()) {
				System.out.println(
						orderShipping.getOrderId() + orderShipping.getShippingCode() + orderShipping.getShippingDate()
								+ orderShipping.getDeliveryDate() + orderShipping.getDeliveryTimeZone()
								+ orderShipping.getUploadStatus());
			}
			// System.out.println(model);
		} catch (OrderShippingValidator e) {
			model.addAttribute("validationError", e.getErrors());
			return "order/shipping";
		} catch (Throwable e) {
			System.out.println("エラー");
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			e.printStackTrace();
			return "redirect:/orders/shipping";
		}
		return "order/shipping";
	}

	@PutMapping("/shipping")
	public String OrderSave(@ModelAttribute("orderShippingData") OrderShippingData orderShippingData, Model model) {
		try {
			System.out.println(orderShippingData);
			System.out.println(orderShippingData.getOrderShippingList());
			for (OrderShipping orderShipping : orderShippingData.getOrderShippingList()) {
				System.out.println(
						orderShipping.getOrderId() + orderShipping.getShippingCode() + orderShipping.getShippingDate()
								+ orderShipping.getDeliveryDate() + orderShipping.getDeliveryTimeZone()
								+ orderShipping.getUploadStatus());
			}
			OrderShippingData orderShippingData2 = new OrderShippingData();
			// List<OrderShipping> orderShippingList = new ArrayList<OrderShipping>();
			orderShippingData2 = orderService.OrderSave(orderShippingData);
			model.addAttribute("orderShippingData", orderShippingData2);
			System.out.println("CSVファイルのインポートが");
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "order/shipping";
	}

	/**
	 * CSVテンプレートダウンロード処理
	 *
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/shipping/download")
	public String download(HttpServletResponse response, RedirectAttributes redirectAttributes) {

		try (OutputStream os = response.getOutputStream();) {
			Path filePath = new ClassPathResource("static/templates/shipping.csv").getFile().toPath();
			byte[] fb1 = Files.readAllBytes(filePath);
			String attachment = "attachment; filename=shipping_" + new Date().getTime() + ".csv";

			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", attachment);
			response.setContentLength(fb1.length);
			os.write(fb1);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// /**
	// * 一括ステータス更新処理
	// *
	// * @param form
	// * @param result
	// * @param redirectAttributes
	// * @return
	// */
	// @PostMapping("/bulkStatusUpdate")
	// public String bulkStatusUpdate(@Validated @ModelAttribute("form")
	// CampaignForm form, BindingResult result,
	// RedirectAttributes redirectAttributes) {
	// if (result.hasErrors()) {
	// redirectAttributes.addFlashAttribute("error", this.makeErrorMessage(result));
	// return "redirect:/campaigns";
	// }
	// try {
	// orderService.bulkStatusUpdate(form.getCheckedIdList(), form.getNextStatus());
	// redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_UPDATE);
	// return "redirect:/campaigns";
	// } catch (Exception e) {
	// redirectAttributes.addFlashAttribute("error", e.getMessage());
	// e.printStackTrace();
	// return "redirect:/campaigns";
	// }
	// }

	/**
	 * エラーメッセージを作成する
	 *
	 * @param result
	 * @return
	 */
	private String makeErrorMessage(BindingResult result) {
		return String.join("<br/>", result.getAllErrors().stream().map(c -> c.getDefaultMessage())
				.collect(Collectors.toList()));
	}

}
