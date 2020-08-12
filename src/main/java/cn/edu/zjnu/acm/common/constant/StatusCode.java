package cn.edu.zjnu.acm.common.constant;

/**
 * 业务状态码
 */
public interface StatusCode {
    int HTTP_SUCCESS = 200;
    // 50008: Illegal token; 50012: Other clients logged in; 50014: Token expired;
    int HTTP_FAILURE = 500;
}
