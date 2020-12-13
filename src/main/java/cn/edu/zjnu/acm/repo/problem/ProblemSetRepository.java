package cn.edu.zjnu.acm.repo.problem;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.ProblemSet;
import cn.edu.zjnu.acm.entity.oj.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, Long> {
    Optional<ProblemSet> findProblemSetByIdAndActive(Long id, Boolean active);

    Page<ProblemSet> findAll(Pageable pageable);

    Page<ProblemSet> findProblemSetByActive(Pageable pageable, Boolean active);

    Page<ProblemSet> findAllByTitleContaining(Pageable pageable, String title);

    Page<ProblemSet> findAllByCreatorAndTitleContaining(Pageable pageable, User user, String title);

    List<ProblemSet> findAllByCreator(User user);

    List<ProblemSet> findAllByCreatorOrActive(User user, Boolean active);

    List<ProblemSet> findAllByActiveAndCreatorNot(Boolean active, User user);

    List<ProblemSet> findAllByActiveAndTitleContaining(Boolean active, String title);

    List<ProblemSet> findAllByTags(Tag tag);

    Optional<ProblemSet> findByTitle(String title);

    Optional<ProblemSet> findById(Long id);
}
