package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Override
    Optional<User> findById(Long aLong);

    @Override
    List<User> findAll();

    @Override
    User save(User entity);

    Optional<User> findByUsername(String username);

    Page<User> findAllByUsernameContains(Pageable pageable, String username);

    @Transactional
    @Modifying
    @Query(value = "update user SET email=:email ,intro=:intro ,name=:uname , password=:password, avatar=:avatar WHERE id=:id"
            , nativeQuery = true)
    int updateUser(@Param("id") Long id, @Param(value = "uname") String name,
                    @Param("password") String password, @Param("email") String email,
                    @Param("intro") String intro,
                    @Param("avatar") String avatar);
}
