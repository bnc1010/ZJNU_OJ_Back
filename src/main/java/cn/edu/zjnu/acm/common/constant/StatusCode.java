package cn.edu.zjnu.acm.common.constant;

/**
 * 业务状态码
 */
public interface StatusCode {
    int HTTP_SUCCESS = 200;

    int HTTP_FAILURE = 500;

    int NOT_FOUND = 404;

    int NEED_LOGIN = 401;

    int NO_PRIVILEGE = 403;
}
