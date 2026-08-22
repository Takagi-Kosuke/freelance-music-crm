package com.freelancemusiccrm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.category.OrderCategoryResponseDto;
import com.freelancemusiccrm.dto.category.OrderCategoryUpsertDto;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.exception.UnprocessableEntityException;
import com.freelancemusiccrm.repository.OrderCategoryRepository;
import com.freelancemusiccrm.repository.TaskRepository;

@Service
public class OrderCategoryService {

    private final OrderCategoryRepository orderCategoryRepository;
    private final TaskRepository taskRepository;

    public OrderCategoryService(OrderCategoryRepository orderCategoryRepository, TaskRepository taskRepository) {
        this.orderCategoryRepository = orderCategoryRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderCategoryResponseDto> findAll() {
        return orderCategoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OrderCategoryResponseDto create(OrderCategoryUpsertDto request) {
        OrderCategory category = new OrderCategory();
        category.setName(request.name());
        category.setDefault(false);

        OrderCategory saved = orderCategoryRepository.save(category);
        return toDto(saved);
    }

    @Transactional
    public OrderCategoryResponseDto update(Long id, OrderCategoryUpsertDto request) {
        OrderCategory category = orderCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("依頼区分が見つかりません"));

        category.setName(request.name());

        OrderCategory saved = orderCategoryRepository.save(category);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("依頼区分が見つかりません");
        }

        if (taskRepository.existsByOrderCategoryId(id)) {
            throw new UnprocessableEntityException("使用中の区分は削除できません");
        }

        orderCategoryRepository.deleteById(id);
    }

    private OrderCategoryResponseDto toDto(OrderCategory category) {
        return new OrderCategoryResponseDto(
                category.getId(),
                category.getName(),
                category.isDefault()
        );
    }
}
