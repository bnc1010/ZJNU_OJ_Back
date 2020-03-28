package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.ve.RoleVO;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.service.RoleService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(description = "角色管理", tags = "RoleController", basePath = "/usermanager")
@Controller
@Slf4j
@RequestMapping("/api/usermanager/role")
public class RoleController{

    @Autowired
    private TokenManager tokenManager;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;

    @ApiOperation(value = "查询列表")
    @RequestMapping(value = "all", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult getRoleList(@RequestBody RoleVO requestRole) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestRole.getToken();
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            String [] aus = tokenModel.getRoleCode().split("&");
            List<Role> roleList = new ArrayList<>();
            if (tokenModel.getRoleCode().contains("a1")){
                roleList = roleService.findAll();
            }
            else{
                for (String au:aus){
                    if (au.equals("ru:"))continue;
                    Role role = roleService.findById(Long.parseLong(au.substring(1)));
                    roleList.add(role);
                }
            }

            restfulResult.setData(roleList);
        } catch (Exception e) {
            restfulResult.setCode(500);
            restfulResult.setMessage("Request role list Failed！");
            log.info("查询列表失败！", e);
        }
        return restfulResult;
    }


    @ApiOperation(value = "添加角色",notes ="参数：roleName角色名, rType角色类型，rRank")
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult addRole(@RequestBody RoleVO requestRole) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            Role role = new Role();
            role.setName(requestRole.getName());
            role.setType(requestRole.getType());
            role.setLevel(requestRole.getLevel());
            roleService.insert(role);
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("Add role Failed！");
            log.info("添加失败！", e);
        }
        return restfulResult;
    }


    @ApiOperation(value = "删除角色", notes = "参数：rId")
    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult deleteRole(@RequestBody RoleVO requestRole) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            Role role = roleService.findById(requestRole.getId());
            List<Long> uIds = roleService.getUserIdByRoleId(role.getId());

            for (long uId : uIds){
                User user = userService.getUserById(uId);
                if (user.getLevel() >= role.getLevel()){
                    List<Role> roles = roleService.getRoleByUserId(user.getId());
                    int minLevel = 1000;
                    for (Role _role : roles){
                        if (_role.getId()!=role.getId()){
                            minLevel = minLevel > _role.getLevel() ? _role.getLevel() : minLevel;
                        }
                    }
                    if (minLevel != role.getLevel()){
                        user.setLevel(minLevel);
                        userService.updateUserInfo(user);
                    }
                }
            }
            roleService.robRole(role.getId());
            roleService.deleteByRoleId(requestRole.getId());
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("Delete role Failed！");
            log.info("删除失败！", e);
        }
        return restfulResult;
    }


    @ApiOperation(value = "给角色赋权限", notes = "参数：rId,权限码数组,token")
    @RequestMapping(value = "grant", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult grantPrivilege(@RequestBody RoleVO requestRole) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestRole.getToken();
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            String [] aus = tokenModel.getPermissionCode().split("&");

            List<Long> pIds = new ArrayList<>();
            for (long rs:requestRole.getPIds()){
                boolean ff = false;
                for (String au:aus){
                    if (au.equals("au:")){
                        continue;
                    }
                    if (rs == Long.parseLong(au.substring(1))){
                        pIds.add(rs);
                        ff = true;
                        break;
                    }
                }
                if (!ff){
                    restfulResult.setCode(500);
                    restfulResult.setMessage("存在越权行为！");
                    return restfulResult;
                }
            }
            roleService.grantPrivileges(requestRole.getId(),pIds);
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("Grant role Failed！");
            log.info("赋权失败！", e);
        }
        return restfulResult;
    }

    @ApiOperation(value = "给角色去权限", notes = "参数：roleId,permissionId")
    @RequestMapping(value = "/drop", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult drop(@RequestBody RoleVO requestRole) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            Map<Long, Boolean> map = new HashMap<>();
            List<Long> pIds = roleService.getPermissionIdByRoleId(requestRole.getId());
            for (long pid : pIds){
                map.put(pid, true);
            }
            for (long rp : requestRole.getPIds()){
                if (!map.containsKey(rp)){
                    restfulResult.setCode(500);
                    restfulResult.setMessage("该角色没有权限，permissionId:" + rp);
                    return restfulResult;
                }
            }
            for (long rp : requestRole.getPIds()){
                roleService.deleteByRoleIdAndPermissionId(requestRole.getId(), rp);
            }
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("drop permission of role Failed！");
            log.info("去权限失败！", e);
        }
        return restfulResult;
    }
}