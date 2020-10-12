package cn.edu.zjnu.acm.authorization.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Token的Model类
 */
@Data
public class TokenModel{

    /**
     * 用户id
     */
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

    public TokenModel(){}

    public TokenModel(long userId, String uuid, String timestamp, String permissionCode, String roleCode, String salt) {
        this.userId = userId;
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.permissionCode = permissionCode;
        this.roleCode = roleCode;
        this.salt = salt;
    }

    public TokenModel(TokenVO tokenVO){
        this.userId = tokenVO.getUserId();
        this.uuid = tokenVO.getUuid();
        this.timestamp = tokenVO.getTimestamp();
        this.permissionCode = tokenVO.getPermissionCode();
        this.roleCode = tokenVO.getRoleCode();
        this.salt = tokenVO.getSalt();
    }

    public String getToken() {
        return userId + "_" + timestamp + "_" + uuid + "_" + permissionCode + "_" + roleCode + "_" + salt;
    }
}
