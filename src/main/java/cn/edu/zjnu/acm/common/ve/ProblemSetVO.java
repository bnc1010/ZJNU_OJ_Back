package cn.edu.zjnu.acm.common.ve;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProblemSetVO {
    private String title;
    private String description;
    private Boolean active;
    private Set<Long> problems;
    private String [] tags;
    private Boolean isPrivate;
}
