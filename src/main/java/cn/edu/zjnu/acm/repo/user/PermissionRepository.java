package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Transactional
    @Query(value = "select * from permission where permission.name=:pname and url=:url"
            , nativeQuery = true)
    Permission findPermissionByNameAndUrl(@Param("pname") String pname,
                            @Param("url") String url);

}
