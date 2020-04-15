package cn.edu.zjnu.acm.authorization.manager.impl;

import cn.edu.zjnu.acm.authorization.manager.AuthorityManager;
import cn.edu.zjnu.acm.entity.Permission;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.repo.user.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthorityManagerImpl implements AuthorityManager {
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;


    public AuthorityManagerImpl(PermissionRepository permissionRepository, UserRoleRepository userRoleRepository, RolePermissionRepository rolePermissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * @param permissionCode 权限码
     * @param target:target api url
     * 检查权限
     **/
    @Override
    public boolean checkAuthority(String permissionCode, String target) {
        String [] permissionCodes = permissionCode.split("&");
        try{
            for (String pc : permissionCodes){
                if (pc.equals("au:")){
                    continue;
                }
                Optional<Permission> optional = permissionRepository.findById(Long.parseLong(pc.substring(1)));
                if (optional.isPresent() && optional != null){
                    Permission permission = optional.get();
                    if (permission.getUrl() != null){
                        if(target.matches(permission.getUrl())){
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }


    /**
     *
     * @param userId
     * @return 权限码和角色码
     */
    @Override
    public String [] getAuthorityCode(long userId) {
        String [] ret = new String[2];
        Map<String, Boolean> mp = new HashMap<>();
        List<Long> roles = userRoleRepository.findRoleIdByUserId(userId);
        for (long roleId : roles){
            List<Long> permissionIds = rolePermissionRepository.findPermissionIdByRoleId(roleId);
            for (long permissionId : permissionIds){
                Optional<Permission> optional = permissionRepository.findById(permissionId);
                if (optional != null && optional.isPresent()){
                    Permission permission = optional.get();
                    if (!mp.containsKey(permission.getType() + permission.getId())){
                        mp.put(permission.getType() + permission.getId(), true);
                    }
                }
            }
        }
        StringBuilder authorityCode = new StringBuilder("au:");
        for (String key : mp.keySet()){
            authorityCode.append("&");
            authorityCode.append(key);
        }
        ret[0] = authorityCode.toString();

        authorityCode = new StringBuilder("ru:");
        for (long roleId : roles){
            Optional<Role> optional = roleRepository.findById(roleId);
            if (optional != null && optional.isPresent()){
                Role _role = optional.get();
                authorityCode.append("&");
                authorityCode.append(_role.getType()).append(_role.getId());
            }
        }
        ret[1] = authorityCode.toString();
        return ret;
    }
}
