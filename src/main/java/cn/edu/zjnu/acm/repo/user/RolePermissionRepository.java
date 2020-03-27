package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    @Transactional
    @Modifying
    @Query(value = "SELECT permission_id FROM role_permission WHERE role_id=:role_id"
            , nativeQuery = true)
    List<Long> findPermissionIdByRoleId(@Param("role_id") Long role_id);

    @Transactional
    @Modifying
    @Query(value = "delete from role_permission where permission_id =:permission_id"
            , nativeQuery = true)
    void robPermission(@Param("permission_id") Long permission_id);

    @Transactional
    @Modifying
    @Query(value = "delete from role_permission where permission_id =:permission_id and role_id=:role_id"
            , nativeQuery = true)
    void deleteByRoleIdAndPermissionId(@Param("permission_id") Long permission_id,
                                       @Param("role_id")Long role_id);


}
