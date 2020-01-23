package cn.edu.zjnu.learncs.repo;

import cn.edu.zjnu.learncs.entity.oj.Problem;
import cn.edu.zjnu.learncs.entity.oj.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findProblemByIdAndActive(Long id, Boolean active);

    Page<Problem> findAll(Pageable pageable);

    Page<Problem> findProblemsByActive(Pageable pageable, Boolean active);

    List<Problem> findProblemsByActiveAndId(Boolean active, Long id);


    //    @Query("select p from Problem p where p.title like %:title% and p.active = :active")
//    List<Problem> getProblemsByActiveAndTitle(@Param("active") Boolean active, @Param("title") String name);
    List<Problem> findAllByActiveAndTitleContaining(Boolean active, String title);

    Page<Problem> findProblemsByActiveAndTagsInAndIdIn(Pageable pageable, Boolean active, List<Tag> tags, List<Long> ids);

    List<Problem> findAllByTags(Tag tag);
}
