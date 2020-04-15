package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.Permission;
import cn.edu.zjnu.acm.entity.Role;
import cn.edu.zjnu.acm.entity.RolePermission;
import cn.edu.zjnu.acm.repo.user.RolePermissionRepository;
import cn.edu.zjnu.acm.repo.user.RoleRepository;
import cn.edu.zjnu.acm.repo.user.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * grant permissions to one role
     * @param roleId
     * @param permissionIds
     */
    @Transactional
    public void grantPrivileges(long roleId, List<Long> permissionIds) {
        for (long pid : permissionIds){
            RolePermission rolePermission = new RolePermission();
            Role role = new Role();
            role.setId(roleId);
            rolePermission.setRole(role);
            Permission permission = new Permission();
            permission.setId(pid);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }
    }

    public boolean checkRoleExist(long roleId) {
        return roleRepository.existsById(roleId);
    }

    public List<Long> getCommonRoleId(){
        return roleRepository.findRoleIdByType("c");
    }

    public List<Role> getCommonRole(){
        return roleRepository.findRoleByType("c");
    }

    public List<Role> findAll(){
        return roleRepository.findAll();
    }

    @Transactional
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

    @Transactional
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
