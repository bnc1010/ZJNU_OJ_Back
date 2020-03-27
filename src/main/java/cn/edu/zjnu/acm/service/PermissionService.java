package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.Permission;
import cn.edu.zjnu.acm.repo.user.PermissionRepository;
import cn.edu.zjnu.acm.repo.user.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("permissionService")
public class PermissionService{
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(PermissionRepository permissionRepository, RolePermissionRepository rolePermissionRepository){
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public long getIdByNameAndUrl(String pName, String pUrl) {
        return permissionRepository.findIdByNameAndUrl(pName,pUrl);
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
        permissionRepository.updateById(permission.getName(), permission.getUrl(), permission.getType());
    }

    public Permission findByPermissionId(long permissionId){
        return permissionRepository.findById(permissionId).get();
    }
}
