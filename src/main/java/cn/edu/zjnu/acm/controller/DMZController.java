package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.AuthorityManager;
import cn.edu.zjnu.acm.authorization.manager.impl.RedisTokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.ve.UserVO;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.entity.User;
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
    private RoleService roleService;



    @ApiOperation(value = "用户登录", notes = "参数：userName,password")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    @IgnoreSecurity
    public RestfulResult login(@RequestBody User requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            User user = userService.loginUser(requestUser);
            String [] authorityCode = authorityManager.getAuthorityCode(user.getId());
//             判断用户是否已经登录过，如果登录过，就将redis缓存中的token删除，重新创建新的token值，保证一个用户在一个时间段只有一个可用 Token
            if (redisTokenManager.hasToken(user.getId())) {
                redisTokenManager.deleteToken(user.getId());
            }
            TokenModel token = redisTokenManager.createToken(user.getId(), authorityCode[0], authorityCode[1], user.getSalt());
            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setName(user.getName());
            userVO.setUsername(user.getUsername());
            userVO.setToken(Base64Util.encodeData(token.getToken()));
//            userVO.setToken(token.getToken());
            restfulResult.setData(userVO);
        } catch (Exception e) {
            e.printStackTrace();
            restfulResult.setCode(500);
            restfulResult.setMessage(e.getMessage());
            log.info("用户登录失败！参数信息：" + requestUser.toString());
        }
        return restfulResult;
    }



    @ApiOperation(value = "用户注册", notes = "参数：userName,password")
    @ResponseBody
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @IgnoreSecurity
    public RestfulResult add(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        User user = new User();
        try {
            user.setEmail(requestUser.getEmail());
            user.setName(requestUser.getName());
            user.setPassword(requestUser.getPassword());
            user.setUsername(requestUser.getUsername());
            user.setIntro(requestUser.getIntro());

            userService.registerUser(user);
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("注册失败！" + e.getMessage());
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
    @IgnoreSecurity
    public RestfulResult logout(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            TokenModel token = redisTokenManager.getToken(Base64Util.decodeData(requestUser.getToken()));
            boolean hasKey = redisTokenManager.deleteToken(token.getUserId());
            if (!hasKey){
                restfulResult.setCode(404);
                restfulResult.setMessage("该用户未登录");
            }
        } catch (Exception e) {
            restfulResult.setCode(500);
            restfulResult.setMessage("Logout failed!");
            log.info("遇到未知错误，退出失败！用户参数：" + requestUser.toString());
        }
        return restfulResult;
    }
}
