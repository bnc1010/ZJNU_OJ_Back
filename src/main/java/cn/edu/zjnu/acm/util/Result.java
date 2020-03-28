package cn.edu.zjnu.acm.util;

import lombok.Data;

public class Result {
    /**
     * 数据集
     */
    public Object data = null;
    /**
     * 返回信息
     */
    public String message = "Request Success！";
    /**
     * 业务自定义状态码
     */
    public Integer code = 200;

    public Object extra = null;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getExtra() {
        return extra;
    }

    public void setExtra(Object etxra) {
        this.extra = etxra;
    }

    public Result(){}

    public Result(Integer code, String message, Object data, Object extra){
        this.code = code;
        this.message = message;
        this.data = data;
        this.extra = extra;
    }

    @Override
    public String toString() {
        return "{" +
                "code=" + code +
                ", message='" + message +
                ", data=" + data +
                ", extra=" + extra +
                '}';
    }
}
