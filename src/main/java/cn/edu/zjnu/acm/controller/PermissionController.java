package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.AuthorityManager;
import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.ve.PermissionVO;
import cn.edu.zjnu.acm.common.ve.RoleVO;
import cn.edu.zjnu.acm.entity.Permission;
import cn.edu.zjnu.acm.service.PermissionService;
import cn.edu.zjnu.acm.service.RoleService;
import cn.edu.zjnu.acm.service.UserOperationService;
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
import java.util.List;

@Api(description = "权限管理", tags = "PermissionHandler", basePath = "/system")
@Controller
@Slf4j
@RequestMapping("/system/permission")
public class PermissionController {
    @Autowired
    private TokenManager tokenManager;
    @Autowired
    private AuthorityManager authorityManager;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private UserOperationService userOperationService;
    @Autowired
    private RoleService roleService;

    @ApiOperation(value = "查询列表", notes = "参数：token")
    @RequestMapping(value = "all", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult getPermissionList(@RequestBody PermissionVO requestPermission) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestPermission.getToken();
            userOperationService.checkTokenNotEmpty(requestPermission.getToken());
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            String [] pus = tokenModel.getPermissionCode().split("&");
            List<Permission> permissionsList = new ArrayList<>();
            for (String pu : pus){
                if (pu.equals("au:"))continue;
                Permission permission = permissionService.findByPermissionId(Long.parseLong(pu.substring(1)));
                if (permission != null){//删除权限后，权限码不会即时更新，要判断权限是否为空
                    permissionsList.add(permission);
                }
            }
            restfulResult.setData(permissionsList);
        } catch (Exception e) {
            restfulResult.setCode(500);
            restfulResult.setMessage(e.getMessage());
            log.info("查询列表失败！", e);
        }
        return restfulResult;
    }

    @ApiOperation(value = "查询角色拥有的权限", notes = "参数：token")
    @RequestMapping(value = "get", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult getPermissionByRoleId(@RequestBody RoleVO requestRole) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            String tk = requestRole.getToken();
            userOperationService.checkTokenNotEmpty(requestRole.getToken());
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            List permissions = roleService.getPermissionIdByRoleId(requestRole.getId());
            restfulResult.setData(permissions);
        } catch (Exception e) {
            restfulResult.setCode(500);
            restfulResult.setMessage("Request permission Failed！");
            log.info("查询失败" + requestRole.getId(), e);
        }
        return restfulResult;
    }



    @ApiOperation(value = "添加权限",notes ="参数：Name,Url,Type,token。添加权限后系统管理员角色权限增加，将更新token，回传一个新token")
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult addPermission(@RequestBody PermissionVO requestPermission) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            Permission permission = new Permission();
            permission.setName(requestPermission.getName());
            permission.setUrl(requestPermission.getUrl());
            permission.setType(requestPermission.getType());
            permissionService.insertPermission(permission.getName(),permission.getUrl(),permission.getType());
            long pId = permissionService.getIdByNameAndUrl(permission.getName(),permission.getUrl());
            List pList = new ArrayList<Integer>();pList.add(pId);
            roleService.grantPrivileges(1,pList);
            String tk = requestPermission.getToken();
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            String [] authorityCode = authorityManager.getAuthorityCode(tokenModel.getUserId());
            tokenManager.deleteToken(tokenModel.getUserId());
            TokenModel token = tokenManager.createToken(tokenModel.getUserId(), authorityCode[0], authorityCode[1]);
            restfulResult.setData(Base64Util.encodeData(token.getToken()));
        }
        catch (Exception e){
            restfulResult.setCode(500);
            if (e.getMessage().contains("SQLIntegrityConstraintViolationException")){
                restfulResult.setMessage("该表达式已有对应权限，添加失败！");
            }
            else{
                restfulResult.setMessage("Add permission Failed！");
            }
            log.info("添加失败！", e);
        }
        return restfulResult;
    }


    @ApiOperation(value = "删除权限", notes = "参数：pId")
    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult deletePermission(@RequestBody PermissionVO requestPermission) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            permissionService.robPermission(requestPermission.getId());
            permissionService.deleteById(requestPermission.getId());
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("Delete permission Failed！");
            log.info("删除失败！", e);
        }
        return restfulResult;
    }

    @ApiOperation(value = "修改权限", notes = "参数：Id，Name,Url,Type")
    @RequestMapping(value = "update", method = RequestMethod.POST)
    @ResponseBody
    public RestfulResult updatePermission(@RequestBody PermissionVO requestPermission) {
        RestfulResult restfulResult = new RestfulResult();
        try {
            Permission permission = new Permission();
            permission.setId(requestPermission.getId());
            permission.setName(requestPermission.getName());
            permission.setUrl(requestPermission.getUrl());
            permission.setType(requestPermission.getType());
            permissionService.updateByPrimaryKey(permission);
        }
        catch (Exception e){
            restfulResult.setCode(500);
            restfulResult.setMessage("Edit permission Failed！");
            log.info("编辑失败！", e);
        }
        return restfulResult;
    }
}