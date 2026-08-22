package com.freelancemusiccrm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.order.OrderActionResponseDto;
import com.freelancemusiccrm.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/token/{token}/approve")
    public ResponseEntity<OrderActionResponseDto> approve(@PathVariable String token) {
        return ResponseEntity.ok(orderService.approveByToken(token));
    }

    @PostMapping("/token/{token}/decline")
    public ResponseEntity<OrderActionResponseDto> decline(@PathVariable String token) {
        return ResponseEntity.ok(orderService.declineByToken(token));
    }
}
