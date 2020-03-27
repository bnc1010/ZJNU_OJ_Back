package cn.edu.zjnu.acm.common.constant;

/**
 * 常量
 */
public class Constants {

    /**
     * 存储当前登录用户id的字段名
     */
    public static final String CURRENT_USER_ID = "CURRENT_USER_ID";

    /**
     * token有效期（小时）
     */
    public static final int TOKEN_EXPIRES_HOUR = 2;

    /**
     * 存放Token的header字段
     */
    public static final String DEFAULT_TOKEN_NAME = "Access-Token";

    /**
     * JWT 加密秘钥
     */
    public static final String SECRET_KEY = "57b5f298-3d3a-41ae-b0aa-925be9d57449";

}
