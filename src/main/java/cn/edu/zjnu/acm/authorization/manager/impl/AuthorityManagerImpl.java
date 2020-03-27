package cn.edu.zjnu.acm.authorization.manager.impl;

import cn.edu.zjnu.acm.authorization.manager.AuthorityManager;
import cn.edu.zjnu.acm.entity.Permission;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.repo.user.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//        System.out.println(authorityCode);
        String [] permissionCodes = permissionCode.split("&");
        for (String pc : permissionCodes){
            if (pc.equals("au:")){
                continue;
            }
            Permission permission = permissionRepository.getOne(Long.parseLong(pc.substring(1)));
            if (permission == null)return false;
            if (permission.getUrl() != null){
                if(target.matches(permission.getUrl())){
                    return true;
                }
            }
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
                Permission permission = permissionRepository.getOne(permissionId);
                if (!mp.containsKey(permission.getType())){
                    mp.put(permission.getType() + permission.getId(), true);
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
            Role _role = roleRepository.getOne(roleId);
            authorityCode.append("&");
            authorityCode.append(_role.getType()).append(_role.getId());
        }
        ret[1] = authorityCode.toString();
        return ret;
    }
}
