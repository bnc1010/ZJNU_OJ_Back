package cn.edu.zjnu.acm.util;

import lombok.Data;

@Data
public class Result {
    /**
     * 数据集
     */
    protected Object data = null;
    /**
     * 返回信息
     */
    protected String message = "Request Success！";
    /**
     * 业务自定义状态码
     */
    protected Integer code = 200;
    /**
     * 全局附加数据
     */
    protected Object extra = null;

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
}
