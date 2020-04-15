package cn.edu.zjnu.acm.common.exception;

public class CommonException extends RuntimeException {
    private String message;
    public CommonException(String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String msg) {
        this.message = msg;
    }
}
