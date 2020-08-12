package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.common.exception.CommonException;
import cn.edu.zjnu.acm.entity.Permission;
import cn.edu.zjnu.acm.repo.user.PermissionRepository;
import cn.edu.zjnu.acm.repo.user.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("permissionService")
public class PermissionService{
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(PermissionRepository permissionRepository, RolePermissionRepository rolePermissionRepository){
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public long getIdByNameAndUrl(String pName, String pUrl) {
        return permissionRepository.findPermissionByNameAndUrl(pName,pUrl).getId();
    }


    @Transactional
    public void deletePermission(long permissionId){
        if (!permissionRepository.existsById(permissionId)){
            throw new CommonException("权限不存在,id:" + permissionId);
        }
        robPermission(permissionId);
        deleteById(permissionId);
    }

    public void robPermission(long permissionId) {
        rolePermissionRepository.robPermission(permissionId);
    }

    public void insertPermission(String pName, String pUrl, String pType) {
        permissionRepository.save(new Permission(pName,pUrl,pType));

    }

    public void deleteById(long permissionId){
        permissionRepository.deleteById(permissionId);
    }

    public void updateByPrimaryKey(Permission permission){
        permissionRepository.save(permission);
    }

    public Permission findByPermissionId(long permissionId){
        Optional<Permission> optional = permissionRepository.findById(permissionId);
        if (optional != null && optional.isPresent()){
            return optional.get();
        }
        else{
            return null;
        }

    }
}
