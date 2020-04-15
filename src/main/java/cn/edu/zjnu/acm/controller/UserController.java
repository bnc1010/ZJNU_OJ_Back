package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.utils.MD5Util;
import cn.edu.zjnu.acm.common.ve.UserVO;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.service.RoleService;
import cn.edu.zjnu.acm.service.UserOperationService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(description = "user管理", tags = "UserHandler", basePath = "/users")
@Controller
@RequestMapping("/api/usermanager/user")
public class UserController{

    @Autowired
    UserService userService;

    @Autowired
    UserOperationService userOperationService;

    @Autowired
    RoleService roleService;

    @ApiOperation(value = "查询列表", notes = "可选参数：pageNum,pageSize")
    @RequestMapping(value = "/all", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult getUserList(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            List<User> userList = null;
//            int pageNum = 1;
//            int pageSize = 10;
//            if (requestUser.getPageNum() != null && requestUser.getPageSize() != null){
//                pageNum = requestUser.getPageNum();
//                pageSize = requestUser.getPageSize();
//            }
            userList = userService.userList();
            restfulResult.setData(userList);
//            restfulResult.setExxra(new PageInfo<>(userList));
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage("Request User list Failed！");
            log.info("查询列表失败！", e);
        }
        return restfulResult;
    }

    @ApiOperation(value = "根据id查询指定的User", notes = "参数:uId")
    @ResponseBody
    @RequestMapping(value = "/get", method = RequestMethod.POST)
    public RestfulResult getUser(@RequestBody UserVO requestUser) {
        RestfulResult resultBean = new RestfulResult();
        try {
            User user = userService.getUserById(requestUser.getId());
            resultBean.setData(user);
        } catch (Exception e) {
            resultBean.setCode(StatusCode.HTTP_FAILURE);
            resultBean.setMessage("Failed to request User details！");
            log.info("查询指定的User失败！参数信息：id = " + requestUser.getId());
        }
        return resultBean;
    }

    @ApiOperation(value = "新增User", notes = "参数:userName,password")
    @ResponseBody
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public RestfulResult add(@RequestBody User user) {
        RestfulResult resultBean = new RestfulResult();
        try{
            userService.registerUser(user);
        }
        catch (Exception e){
            resultBean.setCode(StatusCode.HTTP_FAILURE);
            resultBean.setMessage(e.getMessage());
            log.info("新增User失败！参数信息：User = " + user.toString());
        }
        return resultBean;
    }

    @ApiOperation(value = "根据id查询指定的User", notes = "参数:uId")
    @ResponseBody
    @RequestMapping(value = "/roles", method = RequestMethod.POST)
    public RestfulResult getUserRole(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestUser.getToken();
            userOperationService.checkOperationToUserByToken(tk, requestUser.getId());
            List<Long> rIds = roleService.getRoleIdByUserId(requestUser.getId());
            restfulResult.setData(rIds);
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage(e.getMessage());
            log.info("查询指定的User失败！参数信息：id = " + requestUser.getId(), e);
        }
        return restfulResult;
    }


    @ApiOperation(value = "更新指定的User", notes = "uId,需要更改的字段")
    @ResponseBody
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public RestfulResult update(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestUser.getToken();
            userOperationService.checkOperationToUserByToken(tk, requestUser.getId());
            User oldUser = userService.getUserById(requestUser.getId());

            oldUser.setUsername(requestUser.getUsername());
            oldUser.setName(requestUser.getName());
            oldUser.setIntro(requestUser.getIntro());
            oldUser.setEmail(requestUser.getEmail());

            userService.updateUserInfo(oldUser);
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage(e.getMessage());
            log.info("更新失败！参数信息：id = " + requestUser.getId() + ",User = " + requestUser.toString(), e);
        }
        return restfulResult;
    }



    @ApiOperation(value = "根据id物理删除指定的Role，需谨慎！", notes = "参数：uIds,token")
    @ResponseBody
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public RestfulResult delete(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestUser.getToken();
            boolean allOk = true;
            for(long uId : requestUser.getUserIds()){
                userOperationService.checkOperationToUserByToken(tk, uId);
            }
            for(long uId_ : requestUser.getUserIds()){
//                    userService.deleteByUserId(uId_);
//                    userService.deleteByPrimaryKey(uId_);
            }
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage(e.getMessage());
            log.info("删除失败！参数信息：id = ", e);
        }
        return restfulResult;
    }

    @Autowired
    private TokenManager tokenManager;

    @ApiOperation(value = "根据uid赋角色", notes = "参数：uId，roleCode数组,token")
    @ResponseBody
    @RequestMapping(value = "/grant", method = RequestMethod.POST)
    public RestfulResult grantUser(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestUser.getToken();
            if (tk == null){
                restfulResult.setCode(StatusCode.HTTP_FAILURE);
                restfulResult.setMessage("token无效");
                return restfulResult;
            }
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            String [] rus = tokenModel.getRoleCode().split("&");
            List<Long> rIds = new ArrayList<>();
            int minRank = 10000;
            User nowUser = userService.getUserById(tokenModel.getUserId());
            User targetUser = userService.getUserById(requestUser.getId());
            if (targetUser == null){
                restfulResult.setCode(StatusCode.HTTP_FAILURE);
                restfulResult.setMessage("操作对象不存在!");
                return restfulResult;
            }

            if (tokenModel.getRoleCode().contains("r1")){//系统管理员情况
                for (Long rs:requestUser.getRoleIds()){
                    Role role = roleService.findById(rs);
                    if (role != null){
                        minRank = minRank > role.getLevel() ? role.getLevel() : minRank;
                        rIds.add(role.getId());
                    }
                    else{
                        restfulResult.setCode(StatusCode.HTTP_FAILURE);
                        restfulResult.setMessage("角色" + rs + "不存在!");
                        return restfulResult;
                    }
                }
            }
            else{
                if (targetUser.getLevel() <= nowUser.getLevel()){
                    restfulResult.setCode(StatusCode.HTTP_FAILURE);
                    restfulResult.setMessage("无权操作该用户！");
                    return restfulResult;
                }
                else {
                    for (long rs:requestUser.getRoleIds()){
                        boolean ff = false;
                        for (String au:rus){
                            if (au.equals("ru:")){
                                continue;
                            }
                            if (rs == Long.parseLong(au.substring(1))){
                                Role role = roleService.findById(rs);
                                if (role.getLevel() > nowUser.getLevel()){
                                    rIds.add(role.getId());
                                    minRank = minRank > role.getLevel() ? role.getLevel() : minRank;
                                }
                                else{
                                    restfulResult.setCode(StatusCode.HTTP_FAILURE);
                                    restfulResult.setMessage("角色级别过高！");
                                    return restfulResult;
                                }
                                ff = true;
                                break;
                            }
                        }
                        if (!ff){
                            restfulResult.setCode(StatusCode.HTTP_FAILURE);
                            restfulResult.setMessage("存在越权行为！");
                            return restfulResult;
                        }
                    }
                }
            }
            if (minRank < targetUser.getLevel()){
                targetUser.setLevel(minRank);
                userService.updateLevel(targetUser);
            }
            userService.grantRoles(requestUser.getId(),rIds);
        } catch (Exception e) {
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage(e.getMessage());
            e.printStackTrace();
            log.info("赋角色失败 id = " + requestUser.getId(), e);
        }
        return restfulResult;
    }

    @ApiOperation(value = "根据uid去角色", notes = "参数：uId，roleCode数组,token")
    @ResponseBody
    @RequestMapping(value = "/drop", method = RequestMethod.POST)
    public RestfulResult dropRole(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestUser.getToken();
            if (tk == null) {
                restfulResult.setCode(StatusCode.HTTP_FAILURE);
                restfulResult.setMessage("token无效");
                return restfulResult;
            }
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            User nowUser = userService.getUserById(tokenModel.getUserId());
            User targetUser = userService.getUserById(requestUser.getId());

            if (targetUser == null){
                restfulResult.setCode(StatusCode.HTTP_FAILURE);
                restfulResult.setMessage("操作对象不存在!");
                return restfulResult;
            }

            if (targetUser.getLevel() <= nowUser.getLevel()){
                restfulResult.setCode(StatusCode.HTTP_FAILURE);
                restfulResult.setMessage("无权对该用户操作！");
                return restfulResult;
            }

            int minRank = 1000;
            Map map = new HashMap();
            List<Role> roles = roleService.getRoleByUserId(targetUser.getId());
            for (long rs : requestUser.getRoleIds()){
                boolean ff = false;
                for (Role role : roles){
                    if (rs == role.getId()){
                        ff = true;
                        map.put(role.getId(),true);
                        break;
                    }
                }
                if (!ff){
                    restfulResult.setCode(StatusCode.HTTP_FAILURE);
                    restfulResult.setMessage("该用户无角色,code:" + rs);
                    return restfulResult;
                }
            }
            for (Role role : roles){
                if (!map.containsKey(role.getId())){
                    minRank = minRank > role.getLevel() ? role.getLevel() : minRank;
                }
            }
            if (minRank != targetUser.getLevel()){
                targetUser.setLevel(minRank);
                userService.updateLevel(targetUser);
            }
            for (long rs : requestUser.getRoleIds()){
                userService.robOneRoleOfUser(targetUser.getId(), rs);
            }
        }
        catch (Exception e){
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage("drop role of User failed！");
            e.printStackTrace();
            log.info("去角色失败 id = " + requestUser.getId(), e);
        }
        return restfulResult;
    }

    @ApiOperation(value = "根据uId重置密码", notes = "参数：uId,token")
    @ResponseBody
    @RequestMapping(value = "/reset", method = RequestMethod.POST)
    public RestfulResult RestPassword(@RequestBody UserVO requestUser) {
        RestfulResult restfulResult = new RestfulResult();
        try{
            String tk = requestUser.getToken();
            User u = userOperationService.checkOperationToUserByToken(tk, requestUser.getId());
            u = userService.setUserPassword(u, "123456");
            userService.updateUserInfo(u);
        }
        catch (Exception e){
            restfulResult.setCode(StatusCode.HTTP_FAILURE);
            restfulResult.setMessage(e.getMessage());
            e.printStackTrace();
            log.info("重置密码失败 the id of user is " + requestUser.getId());
        }
        return restfulResult;
    }

}

