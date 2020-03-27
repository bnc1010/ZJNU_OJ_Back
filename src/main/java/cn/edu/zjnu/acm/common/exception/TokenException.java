package cn.edu.zjnu.acm.common.exception;

/**
 * Token过期时抛出异常
 * @author leeyom
 * @date 2017年10月19日 10:41
 */
public class TokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String message;

    public TokenException(String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}