package cn.edu.zjnu.acm.repo.logs;

import cn.edu.zjnu.acm.entity.AdminLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLogs, Long> {
    Page<AdminLogs> findAll(Specification<AdminLogs> specification, Pageable pageable);
}
