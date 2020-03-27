package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Transactional
    @Modifying
    @Query(value = "select id from permission where name=:name and url=:url"
            , nativeQuery = true)
    long findIdByNameAndUrl(@Param("name") String name,
                            @Param("url") String url);

    @Transactional
    @Modifying
    @Query(value = "update permission set name=:name, url=:url, type=:type"
            , nativeQuery = true)
    void updateById(@Param("name") String name,
                    @Param("url") String url,
                    @Param("type") String type);
}
