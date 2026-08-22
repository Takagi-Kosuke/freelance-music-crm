package com.freelancemusiccrm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freelancemusiccrm.entity.OrderCategory;

public interface OrderCategoryRepository extends JpaRepository<OrderCategory, Long> {

    List<OrderCategory> findByIsDefaultTrue();

    List<OrderCategory> findAllByOrderByIdAsc();
}
