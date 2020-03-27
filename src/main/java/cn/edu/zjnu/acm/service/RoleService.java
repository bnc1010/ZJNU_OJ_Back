package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.entity.RolePermission;
import cn.edu.zjnu.acm.repo.user.RolePermissionRepository;
import cn.edu.zjnu.acm.repo.user.RoleRepository;
import cn.edu.zjnu.acm.repo.user.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service("roleService")
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository, UserRoleRepository userRoleRepository){
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public void grantPrivileges(long roleId, List<Long> permissionIds) {
        for (long pid : permissionIds){
            RolePermission rolePermission = new RolePermission();
            rolePermission.setId(pid);
            rolePermission.setId(roleId);
            try {
                rolePermissionRepository.save(rolePermission);
            }
            catch (Exception e){

            }
        }
    }

    public boolean checkRoleExist(long roleId) {
        return roleRepository.existsById(roleId);
    }

    public List<Role> getCommonRole(){
        return roleRepository.findRoleByType("c");
    }

    public List<Role> findAll(){
        return roleRepository.findAll();
    }

    public void robRole(long roleId) {
        userRoleRepository.robRole(roleId);
        roleRepository.deleteByRoleId(roleId);
    }

    public Role findById(long roleId){
        return roleRepository.findById(roleId).get();
    }

    public void insert(Role role){
        roleRepository.save(role);
    }

    public void deleteByRoleId(long roleId){
        roleRepository.deleteByRoleId(roleId);
    }

    public List<Long> getUserIdByRoleId(long roleId) {
        return userRoleRepository.getUserIdByRoleId(roleId);
    }


    public List<Role> getRoleByUserId(long uId) {
        List<Long> rIds = getRoleIdByUserId(uId);
        List<Role> roles = new ArrayList<>();
        for (long rId : rIds){
            roles.add(roleRepository.findById(rId).get());
        }
        return roles;
    }


    public List<Long> getRoleIdByUserId(long userId) {
        return userRoleRepository.findRoleIdByUserId(userId);
    }


    public List<Long> getPermissionIdByRoleId(long roleId) {
        return rolePermissionRepository.findPermissionIdByRoleId(roleId);
    }

    public void deleteByRoleIdAndPermissionId(long roleId, long permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }


}
