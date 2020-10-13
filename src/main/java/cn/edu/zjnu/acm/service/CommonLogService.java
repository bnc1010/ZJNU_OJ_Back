package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.CommonLogs;
import cn.edu.zjnu.acm.repo.logs.CommonLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Slf4j
@Service
public class CommonLogService {
    private final CommonLogRepository commonLogRepository;

    public CommonLogService(CommonLogRepository commonLogRepository){
        this.commonLogRepository = commonLogRepository;
    }

    public void save(CommonLogs commonLogs){
        commonLogRepository.save(commonLogs);
    }

    public Page<CommonLogs> getAllWithSearch(String ip, String url, String startTime, String endTime, long userId, int page, int size){
        return commonLogRepository.findAll( where(ip, url, startTime, endTime, userId), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operateTime")));
    }

    private Specification<CommonLogs> where(String ip, String url, String startTime, String endTime, Long userId) {
        return (Specification<CommonLogs>) (root, query, criteriaBuilder) -> {
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
