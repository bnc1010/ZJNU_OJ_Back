package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.AdminLogs;
import cn.edu.zjnu.acm.repo.logs.AdminLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;

@Service
public class AdminLogService {
    private final AdminLogRepository adminLogRepository;

    public AdminLogService(AdminLogRepository adminLogRepository){
        this.adminLogRepository = adminLogRepository;
    }

    public void save(AdminLogs adminLogs){
        adminLogRepository.save(adminLogs);
    }

    public Page<AdminLogs> getAllWithSearch(String ip, String url, String startTime, String endTime, long userId, int page, int size){
        return adminLogRepository.findAll( where(ip, url, startTime, endTime, userId), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operateTime")));
    }

    private Specification<AdminLogs> where(String ip, String url, String startTime, String endTime, Long userId) {
        return (Specification<AdminLogs>) (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (ip != null && !ip.trim().equals("")){
                predicate.getExpressions().add(criteriaBuilder.like(root.get("ip").as(String.class), ip));
            }
            if (url != null && !url.trim().equals("")){
                predicate.getExpressions().add(criteriaBuilder.like(root.get("url").as(String.class), url));
            }
            if (userId != null && userId != -1){
                predicate.getExpressions().add(criteriaBuilder.equal(root.get("userId").as(Long.class), userId));
            }
            if (startTime != null && !startTime.trim().equals("")) {
                predicate.getExpressions().add(criteriaBuilder.greaterThanOrEqualTo(root.get("operateTime").as(String.class), startTime));
            }
            if (endTime != null && !endTime.trim().equals("")) {
                predicate.getExpressions().add(criteriaBuilder.lessThanOrEqualTo(root.get("operateTime").as(String.class), endTime));
            }
            return predicate;
        };
    }
}
