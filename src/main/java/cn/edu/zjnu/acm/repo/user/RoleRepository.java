package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query(value = "select id from role where role.type=:rtype", nativeQuery = true)
    List<Long> findRoleIdByType(@Param("rtype") String type);

    @Query(value = "select * from role where role.type=:rtype", nativeQuery = true)
    List<Role> findRoleByType(@Param("rtype") String type);

    @Transactional
    @Modifying
    @Query(value = "delete from role where role_id=:role_id", nativeQuery = true)
    void deleteByRoleId(@Param("role_id") Long roleId);

    @Query(value = "select * from role where role.name=:rname", nativeQuery = true)
    Role findRoleByName(@Param("rname") String rname);
}
