package cn.edu.zjnu.acm.util;

public class RestfulResult extends Result {

    public static final String SUCCESS = "success";

    public RestfulResult() {
    }

    public RestfulResult(Integer code, String message, Object data, Object extra) {
        super(code, message, data, extra);
    }

    public RestfulResult(Integer code, String message, Object data) {
        super(code, message, data,null);
    }

    public RestfulResult(int code, String message) {
        super(code, message, null ,null);
    }

    public static RestfulResult successResult() {
        return new RestfulResult(200, SUCCESS, null, null);
    }

}
