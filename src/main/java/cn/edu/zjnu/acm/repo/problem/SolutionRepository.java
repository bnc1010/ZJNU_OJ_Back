/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package cn.edu.zjnu.acm.repo.problem;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {

    Optional<Solution> findById(Long id);

    Page<Solution> findAll(Pageable pageable);

    Page<Solution> findAllByOrderByIdDesc(Pageable pageable);

    Page<Solution> findAllByUser(Pageable pageable, User user);

    List<Solution> findTop5ByUserAndProblemOrderByIdDesc(User user, Problem problem);

    List<Solution> findAllByUser(User user, Sort sort);

    Page<Solution> findAllByProblem(Pageable pageable, Problem problem);

    Page<Solution> findAllByResult(Pageable pageable, String result);

    Page<Solution> findAllByProblemAndResult(Pageable pageable, Problem problem, String result);

    //Top 50
    List<Solution> findFirst50ByResultAndProblemOrderByTimeAsc(String res, Problem problem);

    Page<Solution> findAllByUserAndResult(Pageable pageable, User user, String result);

    Page<Solution> findAllByUserAndProblemAndResult(Pageable pageable, User user, Problem problem, String result);

    Page<Solution> findAllByUserAndProblem(Pageable pageable, User user, Problem problem);

    List<Solution> findAllByUserAndProblemOrderByIdDesc(User user, Problem problem);

    @Transactional
    @Modifying
    @Query(value = "update solution set " +
            "solution.result=:re ," +
            " solution.time=:ti, " +
            "solution.memory=:me , " +
            "solution.case_number=:ca " +
            "where solution.id=:id", nativeQuery = true)
    void updateResultTimeMemoryCase(@Param(value = "id") Long id,
                                    @Param(value = "re") String result,
                                    @Param(value = "ti") int time,
                                    @Param(value = "me") int memory,
                                    @Param(value = "ca") int n_case);

    @Transactional
    @Modifying
    @Query(value = "update solution set " +
            "share = :share " +
            "where id = :id", nativeQuery = true)
    void updateShare(@Param("id") Long id, @Param("share") Boolean share);

    @Transactional
    @Modifying
    @Query(value = "update solution set solution.result = :result , solution.info = :info where solution.id = :id", nativeQuery = true)
    void updateResultInfo(@Param("id") Long id, @Param("result") String result, @Param("info") String info);

    Page<Solution> findAllByContestAndUser(Pageable pageable, Contest contest, User user);

    List<Solution> findAllByContestOrderByIdDesc(Contest contest);

    Long countAllByProblemAndResult(Problem problem, String result);

    Long countAllByProblem(Problem problem);

    Long countAllByContestAndProblem(Contest contest, Problem problem);

    Long countAllByContestAndProblemAndResult(Contest contest, Problem problem, String result);

    Long countAllByUser(User user);

    Long countAllByUserAndResult(User user, String result);

    @Query(nativeQuery = true,
            value = "SELECT COALESCE(SUM(score),0) FROM " +
                    "(SELECT DISTINCT pp.id,pp.score " +
                    "FROM user, solution AS ss, problem AS pp " +
                    "WHERE user.id = ss.user_id AND ss.problem_id=pp.id AND ss.result='Accepted' AND user.id = :uid)t1")
    Long calculateScoreOfUser(@Param("uid") Long user_id);

    void deleteAllByProblem(Problem problem);
}

