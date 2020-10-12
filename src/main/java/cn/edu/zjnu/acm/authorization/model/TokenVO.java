package cn.edu.zjnu.acm.authorization.model;

import lombok.Data;

@Data
public class TokenVO {
    private long userId;

    /**
     * 随机生成的uuid
     */
    private String uuid;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 权限码
     */
    private String permissionCode;

    /**
     * 角色码
     */
    private String roleCode;

    /**
     * 盐
     */
    private String salt;

    public TokenVO(){}

    public TokenVO(TokenModel tokenModel){
        this.userId = tokenModel.getUserId();
        this.uuid = tokenModel.getUuid();
        this.timestamp = tokenModel.getTimestamp();
        this.permissionCode = tokenModel.getPermissionCode();
        this.roleCode = tokenModel.getRoleCode();
        this.salt = tokenModel.getSalt();
    }
}
