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
import cn.edu.zjnu.acm.entity.AdminLogs;
import cn.edu.zjnu.acm.entity.CommonLogs;
import cn.edu.zjnu.acm.service.AdminLogService;
import cn.edu.zjnu.acm.service.CommonLogService;
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

    @Autowired
    CommonLogService commonLogService;

    @Autowired
    AdminLogService adminLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)){
            return true;
        }
        HandlerMethod handlerMethod=(HandlerMethod)handler;
        Method method=handlerMethod.getMethod();

        // *放行swagger相关的请求url，开发阶段打开，生产环境注释掉
        URL requestUrl = new URL(request.getRequestURL().toString());
        if (requestUrl.getPath().contains("configuration")) {
            return true;
        }
        if (requestUrl.getPath().contains("swagger")) {
            return true;
        }
        if (requestUrl.getPath().contains("api-docs")) {
            return true;
        }
        // ***************************************************

        boolean log_c = method.isAnnotationPresent(LogsOfUser.class);
        boolean log_a = method.isAnnotationPresent(LogsOfAdmin.class);

        CommonLogs commonLogs = new CommonLogs();
        AdminLogs adminLogs = new AdminLogs();
        commonLogs.setIp(request.getRemoteAddr());
        commonLogs.setUrl(requestUrl.getPath());
        adminLogs.setIp(request.getRemoteAddr());
        adminLogs.setUrl(requestUrl.getPath());

        if (method.isAnnotationPresent(IgnoreSecurity.class)) {
            if (log_c){
                commonLogs.setResult("success");
                commonLogService.save(commonLogs);
            }
            return true;
        }

        // 从 request header 中获取当前 token
        String authentication = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        if (authentication == null || authentication.equals("")){
            if (log_c){
                commonLogs.setResult("token invalid");
                commonLogService.save(commonLogs);
            }
            if (log_a){
                adminLogs.setResult("token invalid");
                adminLogService.save(adminLogs);
            }
            throw new TokenException("token invalid");
        }
        TokenModel tokenModel = redisService.getToken(authentication);

        if (tokenModel == null){
            try{
                tokenModel = tokenManager.getToken(Base64Util.decodeData(authentication));
            }
            catch (Exception e){
                if (log_c){
                    commonLogs.setResult("token invalid");
                    commonLogService.save(commonLogs);
                }
                if (log_a){
                    adminLogs.setResult("token invalid");
                    adminLogService.save(adminLogs);
                }
                throw new TokenException("token invalid");
            }
            redisService.insertToken(authentication, tokenModel);
        }

        // 检查有效性(检查是否登录)
        if (!tokenManager.checkToken(tokenModel)) {
            String message = "token " + authentication + " is invalid！！！";
            if (log_c){
                commonLogs.setUserId(tokenModel.getUserId());
                commonLogs.setResult("not login");
                commonLogService.save(commonLogs);
            }
            if (log_a){
                adminLogs.setUserId(tokenModel.getUserId());
                adminLogs.setResult("not login");
                adminLogService.save(adminLogs);
            }
            throw new TokenException(message);
        }

        //检查权限
        if (!authorityManager.checkAuthority(tokenModel.getPermissionCode(), requestUrl.getPath())){
            String message = tokenModel.getUserId() + " try to enter " + requestUrl.getPath() + " without permission";
            log.info(message);
            if (log_c){
                commonLogs.setUserId(tokenModel.getUserId());
                commonLogs.setResult("no privilege");
                commonLogService.save(commonLogs);
            }
            if (log_a){
                adminLogs.setUserId(tokenModel.getUserId());
                adminLogs.setResult("no privilege");
                adminLogService.save(adminLogs);
            }
            throw new AuthorityException("无权访问");
        }

        if (log_c){
            commonLogs.setUserId(tokenModel.getUserId());
            commonLogs.setResult("success");
            commonLogService.save(commonLogs);
        }
        if (log_a){
            adminLogs.setUserId(tokenModel.getUserId());
            adminLogs.setResult("success");
            adminLogService.save(adminLogs);
        }
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
