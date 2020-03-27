package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Transactional
    @Modifying
    @Query(value = "SELECT role_id FROM user_role WHERE user_id=:user_id"
            , nativeQuery = true)
    List<Long> findRoleIdByUserId(@Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "delete from user_role where user_id=:user_id"
            , nativeQuery = true)
    void robRole(@Param("user_id") Long user_id);

    @Transactional
    @Modifying
    @Query(value = "SELECT user_id FROM user_role WHERE role_id=:role_id"
            , nativeQuery = true)
    List<Long> getUserIdByRoleId(@Param("role_id") Long role_id);

}
