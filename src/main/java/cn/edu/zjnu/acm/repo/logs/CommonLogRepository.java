package cn.edu.zjnu.acm.repo.logs;

import cn.edu.zjnu.acm.entity.CommonLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonLogRepository extends JpaRepository<CommonLogs, Long> {

    Page<CommonLogs> findAll(Specification<CommonLogs> specification, Pageable pageable);

}
