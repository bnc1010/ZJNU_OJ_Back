package cn.edu.zjnu.acm.common.ve;

import lombok.Data;

@Data
public class PermissionVO {
    private long id;
    private String url;
    private String name;
    private String type;
    private Integer pageNum;
    private Integer pageSize;
    private String token;
}
