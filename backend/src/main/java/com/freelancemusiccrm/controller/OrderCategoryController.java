package com.freelancemusiccrm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.category.OrderCategoryResponseDto;
import com.freelancemusiccrm.dto.category.OrderCategoryUpsertDto;
import com.freelancemusiccrm.service.OrderCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/order-categories")
public class OrderCategoryController {

    private final OrderCategoryService orderCategoryService;

    public OrderCategoryController(OrderCategoryService orderCategoryService) {
        this.orderCategoryService = orderCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<OrderCategoryResponseDto>> findAll() {
        return ResponseEntity.ok(orderCategoryService.findAll());
    }

    @PostMapping
    public ResponseEntity<OrderCategoryResponseDto> create(
            @Valid @RequestBody OrderCategoryUpsertDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderCategoryResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody OrderCategoryUpsertDto request
    ) {
        return ResponseEntity.ok(orderCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
