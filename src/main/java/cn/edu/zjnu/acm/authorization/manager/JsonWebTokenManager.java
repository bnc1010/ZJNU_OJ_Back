package cn.edu.zjnu.acm.authorization.manager;

public interface JsonWebTokenManager {

    /**
     * 创建一个 JWT token
     * @param userId 指定用户的id
     * @param authorityCode 权限码
     * @return 生成的token
     */
    String createToken(long userId, String authorityCode);

    /**
     * 检查 JWT token是否有效
     * @param token token
     * @return 是否有效
     */
    boolean checkToken(String token);

}
