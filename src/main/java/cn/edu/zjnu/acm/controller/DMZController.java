package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.AuthorityManager;
import cn.edu.zjnu.acm.authorization.manager.impl.RedisTokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.annotation.LogsOfUser;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.ve.UserVO;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.service.RedisService;
import cn.edu.zjnu.acm.service.RoleService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Api(description = "开放区", tags = "DMZController")
@Controller
@Slf4j
@RequestMapping("/api/dmz")
public class DMZController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthorityManager authorityManager;
    @Autowired
    private RedisTokenManager redisTokenManager;
    @Autowired
    private RedisService redisService;


    @ApiOperation(value = "用户登录", notes = "参数：userName,password")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    @IgnoreSecurity
    @LogsOfUser
    public RestfulResult login(@RequestBody User requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        User user = null;
        TokenModel token = null;
        try {
            user = userService.loginUser(requestUser);
            String [] authorityCode = authorityManager.getAuthorityCode(user.getId());
//             判断用户是否已经登录过，如果登录过，就将redis缓存中的token删除，重新创建新的token值，保证一个用户在一个时间段只有一个可用 Token
            if (redisTokenManager.hasToken(user.getId())) {
                redisTokenManager.deleteToken(user.getId());
            }
            token = redisTokenManager.createToken(user.getId(), authorityCode[0], authorityCode[1], user.getSalt());
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage(e.getMessage());
            log.info("用户登录失败！参数信息：" + requestUser.toString());
            return restfulResult;
        }
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setName(user.getName());
        userVO.setUsername(user.getUsername());
        userVO.setAvatar(user.getAvatar());
        userVO.setEmail(user.getEmail());
        userVO.setIntro(user.getIntro());
        String tk = Base64Util.encodeData(token.getToken());
        userVO.setToken(tk);
        redisService.insertToken(tk, token);
        restfulResult.setData(userVO);
        return restfulResult;
    }



    @ApiOperation(value = "用户注册", notes = "参数：userName,password")
    @ResponseBody
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @IgnoreSecurity
    @LogsOfUser
    public RestfulResult add(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        User user = new User();
        user.setEmail(requestUser.getEmail());
        user.setName(requestUser.getName());
        user.setPassword(requestUser.getPassword());
        user.setUsername(requestUser.getUsername());
        if (requestUser.getIntro() == null){
            user.setIntro("这人很懒，什么也没留下");
        }
        else{
            user.setIntro(requestUser.getIntro());
        }
        try {
            userService.registerUser(user);
        }
        catch (Exception e){
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage("注册失败！");
            log.info("新增User失败！参数信息：User = " + user.toString());
        }
        return restfulResult;
    }

    /**
     * 登出
     *
     */
    @ApiOperation(value = "用户登出", notes = "参数：token")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    @LogsOfUser
    public RestfulResult logout(@RequestBody UserVO requestUser, HttpServletRequest request) {
        RestfulResult restfulResult = new RestfulResult();
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        try {
            TokenModel token = redisTokenManager.getToken(Base64Util.decodeData(tk));
            boolean hasKey = redisTokenManager.deleteToken(token.getUserId());
            if (!hasKey){
                restfulResult.setCode(StatusCode.NEED_LOGIN);
                restfulResult.setMessage("该用户未登录");
            }
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage("Logout failed!");
            log.info("遇到未知错误，退出失败！用户参数：" + requestUser.toString());
        }
        return restfulResult;
    }


    @ApiOperation(value = "用户身份", notes = "参数：token")
    @RequestMapping(value = "/userinfo", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult userInfo(@RequestBody UserVO requestUser, HttpServletRequest request){
        RestfulResult restfulResult = new RestfulResult();
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel token = redisService.getToken(tk);
        try {
            List roles = authorityManager.getRoleByToken(token.getRoleCode());
            restfulResult.setData(roles);
        } catch (Exception e) {
            e.printStackTrace();
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage("get info failed");
            log.info("遇到未知错误，！用户参数：" + requestUser.toString());
        }
        return restfulResult;
    }
}
