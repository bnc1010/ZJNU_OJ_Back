package cn.edu.zjnu.acm.authorization.model;

/**
 * Token的Model类，可以增加字段提高安全性，例如时间戳、url签名
 */
public class TokenModel {

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

    private String roleCode;

    public TokenModel(long userId, String uuid, String timestamp, String permissionCode, String roleCode) {
        this.userId = userId;
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.permissionCode = permissionCode;
        this.roleCode = roleCode;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getToken() {
        return userId + "_" + timestamp + "_" + uuid + "_" + permissionCode + "_" + roleCode;
    }
}
