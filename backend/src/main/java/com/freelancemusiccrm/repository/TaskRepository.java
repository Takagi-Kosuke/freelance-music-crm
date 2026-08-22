package com.freelancemusiccrm.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freelancemusiccrm.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** 依頼区分フィルタ: Order に紐づく category_id で絞り込む */
    List<Task> findByOrderCategoryId(Long categoryId);

    /** カレンダー期間クエリ: Order の desired_delivery_date が指定期間内のタスクを取得 */
    List<Task> findByOrderDesiredDeliveryDateBetween(LocalDate start, LocalDate end);

    /** Order に紐づく Task を取得 */
    Optional<Task> findByOrderId(Long orderId);

    /** 依頼区分に紐づく Task の存在確認 */
    boolean existsByOrderCategoryId(Long categoryId);
}
