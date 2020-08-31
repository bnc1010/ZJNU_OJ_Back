package cn.edu.zjnu.acm.common.exception;

import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionResolver {
    public static final RestfulResult pleaseLoginResult = new RestfulResult(StatusCode.NEED_LOGIN, "请登录 Please Login", null, null);

    @ExceptionHandler(NeedLoginException.class)
    @ResponseBody
    public RestfulResult exceptionHandle() {
        return pleaseLoginResult;
    }

    @ExceptionHandler(UnavailableException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public RestfulResult unavailableHandle() {
        return new RestfulResult(503, "维护中，不可用", null, null);
    }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public RestfulResult validatorExceptionHandler(Exception e) {
        String msg = e instanceof BindException ? String.valueOf(((BindException) e).getBindingResult())
                : String.valueOf(((ConstraintViolationException) e).getConstraintViolations());
        return new RestfulResult(400, msg, null, null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    public RestfulResult handleBindException(MethodArgumentNotValidException ex) {
        RestfulResult errorResult = new RestfulResult(400, "Bad Request", "");
        StringBuilder msg = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach((e) -> {
            msg.append(e.getDefaultMessage() + "\n");
        });
        errorResult.setCode(400);
        errorResult.setData(msg.toString());
        return errorResult;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public RestfulResult notFoundExceptionHandle(NotFoundException e) {
        return new RestfulResult(StatusCode.NOT_FOUND, e.getMessage(), null, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public RestfulResult forbiddenExceptionHandle(ForbiddenException e) {
        return new RestfulResult(StatusCode.NO_PRIVILEGE, e.getMessage(), null, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public RestfulResult serverExceptionHandle(Exception e) {
        e.printStackTrace();
        return new RestfulResult(StatusCode.HTTP_FAILURE, "Internal Server Error", null, null);
    }
}
