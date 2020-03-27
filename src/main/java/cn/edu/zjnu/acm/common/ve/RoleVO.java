package cn.edu.zjnu.acm.common.ve;

import lombok.Data;

@Data
public class RoleVO {
    private long id;
    private String name;
    private String type;
    private int level;
    private Integer pageNum;
    private Integer pageSize;
    private String [] pCodes;
    private long [] pIds;
    private String token;
}
