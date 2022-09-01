package com.shop.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.shop.dto.OrderDto;
import com.shop.dto.OrderHistDto;
import com.shop.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Controller
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping(value = { "/orders", "/orders/{page}" })
	public String orderHist(@PathVariable("page") Optional<Integer> page, Principal principal, Model model) {
		Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 4); // 한번에 갖고 올 주문 개수 4개
		Page<OrderHistDto> ordersHistDtoList = orderService.getOrderList(principal.getName(), pageable);
		model.addAttribute("orders", ordersHistDtoList);
		model.addAttribute("page", pageable.getPageNumber());
		model.addAttribute("maxPage", 5);
		return "order/orderHist";
	}

	@PostMapping(value = "/order")
	public @ResponseBody ResponseEntity order(@RequestBody @Valid OrderDto orderDto, BindingResult bindingResult,
			Principal principal) {
		// 스프링에서 비동기 처리, @리퀘스트바디 http요청 바디 담긴 내용을 자바 객체로 전달, @리스폰스바디 자바객체를 body로 전달
		if (bindingResult.hasErrors()) {// 주문정보를 받는 오더디티오 객체에 데이터 바인딩시 에러가 있는지 검사
			StringBuilder sb = new StringBuilder();
			List<FieldError> fieldErrors = bindingResult.getFieldErrors();

			for (FieldError fieldError : fieldErrors) {
				sb.append(fieldError.getDefaultMessage());
			}

			return new ResponseEntity<String>(sb.toString(), HttpStatus.BAD_REQUEST);

		} // 에러정보를 리스폰스엔티티 객체에 담아 반환한다
		String email = principal.getName();// 현재 로그인유저의 정보를 얻기위해서 컨트롤러가 선언된 클래스에서 메서드 인자로
		// principal객체를 넘겨줄 경우, 해당 객체에 직접 접근할 수 있다. principal객체에서 현재 로그인한 회원의 이메일 정보를
		// 조회한다
		Long orderId;

		try {
			orderId = orderService.order(orderDto, email);
		} catch (Exception e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Long>(orderId, HttpStatus.OK);
		// 결과값으로 생성된 주문번호와 요청이 성공했다는 http응답상태코드를 반환한다
	}

	@PostMapping("/order/{orderId}/cancel")
	public @ResponseBody ResponseEntity cancelOrder(@PathVariable("orderId") Long orderId, Principal principal) {
		if (!orderService.validateOrder(orderId, principal.getName())) {
			return new ResponseEntity<String>("주문 취소 권한이 없습니다.", HttpStatus.FORBIDDEN);
		} // 자바스크립트에서 취소할 주문 번호는 조작이 가능하므로 다른 사람의 주문을 취소하지 못하도록 주문 취소 권한을 검사한다.
	    orderService.cancelOrder(orderId); // 주문 취소 로직을 호출
		return new ResponseEntity<Long>(orderId, HttpStatus.OK);
	}
}
