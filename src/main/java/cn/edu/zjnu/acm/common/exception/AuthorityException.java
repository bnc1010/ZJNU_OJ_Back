package cn.edu.zjnu.acm.common.exception;

public class AuthorityException extends RuntimeException {

    private String message;
    public AuthorityException(String message) {
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
