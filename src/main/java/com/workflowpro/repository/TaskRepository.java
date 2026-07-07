package com.workflowpro.repository;

import com.workflowpro.entity.Task;
import com.workflowpro.entity.TaskPriority;
import com.workflowpro.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    @Query("""
            select distinct t from Task t
            left join t.project.members m
            where t.project.owner.id = :userId or m.id = :userId
            """)
    List<Task> findAllAccessibleByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct t from Task t
            left join t.project.members m
            where (t.project.owner.id = :userId or m.id = :userId) and t.status = :status
            order by t.updatedAt desc
            """)
    List<Task> findAccessibleByStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);

    @Query("""
            select distinct t from Task t
            left join t.project.members m
            where (t.project.owner.id = :userId or m.id = :userId) and t.priority = :priority
            order by t.updatedAt desc
            """)
    List<Task> findAccessibleByPriority(@Param("userId") Long userId, @Param("priority") TaskPriority priority);
}
