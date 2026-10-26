package com.example.pandasoft.assignment.repository;

import com.example.pandasoft.assignment.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (key, employee_id, created_at)
            VALUES (:key, :employeeId, CURRENT_TIMESTAMP)
            ON CONFLICT (key) DO NOTHING
            """, nativeQuery = true)
    int reserve(@Param("key") String key, @Param("employeeId") Long employeeId);
}
