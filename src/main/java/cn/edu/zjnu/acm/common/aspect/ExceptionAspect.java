package cn.edu.zjnu.acm.common.aspect;

import cn.edu.zjnu.acm.common.exception.AuthorityException;
import cn.edu.zjnu.acm.common.exception.TokenException;
import cn.edu.zjnu.acm.util.RestfulResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ValidationException;


/**
 * 全局异常处理切面
 */

@ControllerAdvice
@ResponseBody
public class ExceptionAspect {
    

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RestfulResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return new RestfulResult(400,"Could not read json...");
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public RestfulResult handleValidationException(MethodArgumentNotValidException e) {
        return new RestfulResult(400,"参数检验异常！");
    }

    /**
     * 405 - Method Not Allowed。HttpRequestMethodNotSupportedException
     * 是ServletException的子类,需要Servlet API支持
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public RestfulResult handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return new RestfulResult(405, "请求方法不支持！");
    }

    /**
     * 415 - Unsupported Media Type。HttpMediaTypeNotSupportedException
     * 是ServletException的子类,需要Servlet API支持
     */
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    public RestfulResult handleHttpMediaTypeNotSupportedException(Exception e) {
        return new RestfulResult(415, "内容类型不支持！");
    }

    /**
     * 401 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(TokenException.class)
    public RestfulResult handleTokenException(Exception e) {
        return new RestfulResult(401, "Token已失效");
    }

    /**
     * 500 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public RestfulResult handleException(Exception e) {
        return new RestfulResult(500, "内部服务器错误！");
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public RestfulResult handleValidationException(ValidationException e) {
        return new RestfulResult(400, "参数验证失败！");
    }


    /**
     * 403 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(AuthorityException.class)
    public RestfulResult handleAuthorityException(Exception e) {
        return new RestfulResult(403, "无权访问");
    }
}
