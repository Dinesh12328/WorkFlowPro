package com.workflowpro.repository;

import com.workflowpro.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
            select distinct p from Project p
            left join p.members m
            where p.owner.id = :userId or m.id = :userId
            order by p.updatedAt desc
            """)
    List<Project> findAccessibleByUserId(@Param("userId") Long userId);
}
