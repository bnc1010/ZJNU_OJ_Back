package cn.edu.zjnu.acm.interceptor;

import cn.edu.zjnu.acm.authorization.manager.AuthorityManager;
import cn.edu.zjnu.acm.authorization.manager.impl.RedisTokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.annotation.LogsOfAdmin;
import cn.edu.zjnu.acm.common.annotation.LogsOfUser;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.exception.AuthorityException;
import cn.edu.zjnu.acm.common.exception.TokenException;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.net.URL;


@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Autowired
    RedisTokenManager tokenManager;

    @Autowired
    AuthorityManager authorityManager;

    @Autowired
    RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if(!(handler instanceof HandlerMethod)){
            return true;
        }
        HandlerMethod handlerMethod=(HandlerMethod)handler;
        Method method=handlerMethod.getMethod();



        // *******************************放行swagger相关的请求url，开发阶段打开，生产环境注释掉*******************************
        URL requestUrl = new URL(request.getRequestURL().toString());
//        log.info(requestUrl.getPath());
        if (requestUrl.getPath().contains("configuration")) {
            return true;
        }
        if (requestUrl.getPath().contains("swagger")) {
            return true;
        }
        if (requestUrl.getPath().contains("api-docs")) {
            return true;
        }

        // ************************************************************************************************************


        boolean log_c = method.isAnnotationPresent(LogsOfUser.class);
        boolean log_a = method.isAnnotationPresent(LogsOfAdmin.class);

        // 若目标方法忽略了安全性检查,则直接调用目标方法
        if (method.isAnnotationPresent(IgnoreSecurity.class)) {
            return true;
        }
        // 从 request header 中获取当前 token
        String authentication = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = redisService.getToken(authentication);

        if (tokenModel == null){
            try{
                tokenModel = tokenManager.getToken(Base64Util.decodeData(authentication));
            }
            catch (Exception e){
                throw new TokenException("token invalid");
            }
            redisService.insertToken(authentication, tokenModel);
        }

        // 检查有效性(检查是否登录)
        if (!tokenManager.checkToken(tokenModel)) {
            String message = "token " + authentication + " is invalid！！！";
            log.info(message);
            throw new TokenException(message);
        }

        //检查权限
        if (!authorityManager.checkAuthority(tokenModel.getPermissionCode(), requestUrl.getPath())){
            String message = tokenModel.getUserId() + " try to enter " + requestUrl.getPath() + " without permission";
            log.info(message);
            throw new AuthorityException("无权访问");
        }

        // 调用目标方法
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {

    }
}
