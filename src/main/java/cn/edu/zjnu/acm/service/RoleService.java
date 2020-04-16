package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.common.exception.CommonException;
import cn.edu.zjnu.acm.entity.*;
import cn.edu.zjnu.acm.repo.user.RolePermissionRepository;
import cn.edu.zjnu.acm.repo.user.RoleRepository;
import cn.edu.zjnu.acm.repo.user.UserRepository;
import cn.edu.zjnu.acm.repo.user.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service("roleService")
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository, UserRoleRepository userRoleRepository,UserRepository userRepository){
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public void insert(Role role){
        System.out.println(role);
        Role dbRole = roleRepository.save(role);
        if (dbRole == null){
            throw new CommonException("角色插入失败");
        }
        if (role.getType().equals("c")){
            List<User> users = userRepository.findAll();
            for (User u : users){
                userRoleRepository.save(new UserRole(u, dbRole));
                if (u.getLevel() > dbRole.getLevel()){
                    u.setLevel(dbRole.getLevel());
                    userRepository.save(u);
                }
            }
        }
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
