package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.*;
import cn.edu.zjnu.acm.common.exception.ForbiddenException;
import cn.edu.zjnu.acm.common.exception.NeedLoginException;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.CommentRepository;
import cn.edu.zjnu.acm.repo.contest.ContestProblemRepository;
import cn.edu.zjnu.acm.service.*;
import cn.edu.zjnu.acm.util.Rank;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/contest")
class ContestViewController {
    @GetMapping
    public String contestPage() {
        return "contest/contests";
    }

    @GetMapping("/problem/{id:[0-9]+}")
    public String showContest(@PathVariable(value = "id") Long id) {
        return "contest/contestinfo";
    }

    @GetMapping("/status/{id:[0-9]+}")
    public String showContestStatus(@PathVariable(value = "id") Long id) {
        return "contest/conteststatus";
    }

    @GetMapping("/ranklist/{id:[0-9]+}")
    public String showContestRanklist(@PathVariable(value = "id") Long id) {
        return "contest/contestrank";
    }

    @GetMapping("/comment/{id:[0-9]+}")
    public String showContestComment(@PathVariable(value = "id") Long id) {
        return "contest/contestcomment";
    }

    @GetMapping("/{id:[0-9]+}")
    public String contestGate(@PathVariable(value = "id") Long id) {
        return "contest/contestgate";
    }

    @GetMapping("/create/{tid:[0-9]+}")
    public String createContest() {
        return "contest/create_contest";
    }

    @GetMapping("/edit/{tid:[0-9]+}")
    public String editContest() {
        return "contest/edit_contest";
    }
}

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/contest")
public class ContestController {
    private static final int PAGE_SIZE = 30;
    private final HttpSession session;
    private final UserService userService;
    private final ProblemService problemService;
    private final SolutionService solutionService;
    private final ContestService contestService;
    private final JudgeService judgeService;
    private final ContestProblemRepository contestProblemRepository;
    private final TeamService teamService;

    public ContestController(HttpSession session, UserService userService, ProblemService problemService, SolutionService solutionService, ContestService contestService, JudgeService judgeService, ContestProblemRepository contestProblemRepository, TeamService teamService, CommentRepository commentRepository) {
        this.session = session;
        this.userService = userService;
        this.problemService = problemService;
        this.solutionService = solutionService;
        this.contestService = contestService;
        this.judgeService = judgeService;
        this.contestProblemRepository = contestProblemRepository;
        this.teamService = teamService;
    }

    @GetMapping("")
    public Page<Contest> showContests(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(0, page);
        Page<Contest> currentPage = contestService.getContestWithoutTeam(page, PAGE_SIZE, search);
        for (Contest c : currentPage.getContent()) {
            c.clearLazyRoles();
            c.setProblems(null);
            c.setSolutions(null);
            c.setContestComments(null);
            c.setPassword(null);
            c.setCreator(null);
            c.setFreezeRank(null);
            c.setCreateTime(null);
            if (c.getTeam() != null) {
                c.getTeam().setCreator(null);
                c.getTeam().clearLazyRoles();
            }
        }
        return currentPage;
    }

    @GetMapping("/clone/{id:[0-9]+}")
    public RestfulResult cloneContest(@PathVariable Long id) {
        Contest c = contestService.getContestById(id, true);
        if (c == null) {
            return new RestfulResult(404, "no contest found", null);
        }
        c.setSolutions(null);
        c.setContestComments(null);
        c.setCreator(null);
        c.setPassword(null);
        c.setTeam(null);
        return new RestfulResult(200, "success", c);
    }

    @GetMapping("/gate/{cid:[0-9]+}")
    public String contestReady(@PathVariable("cid") Long cid) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null)
            throw new NeedLoginException();
        Contest contest = contestService.getContestById(cid);
        if (contest == null)
            throw new NotFoundException();
        if (!contest.isStarted())
            return "未开始 not started";
        if (userService.getUserPermission(user) == -1) {
            if (contest.getPrivilege().equals(Contest.TEAM)) {
                Team team = contest.getTeam();
                if (teamService.isUserInTeam(user, team)) {
                    return "success";
                } else {
                    return "没有权限";
                }
            }
        }
        return "success";
    }

    private Boolean isContestCreator(Contest contest, User currentUser) {
        if (contest == null) {
            throw new NotFoundException();
        }
        return contest.getCreator().getId() == currentUser.getId();
    }

    @GetMapping("/background/access/{cid:[0-9]+}")
    public String isAccessContestBackground(@PathVariable("cid") Long cid,
                                            @SessionAttribute User currentUser) {
        Contest contest = contestService.getContestById(cid);
        if (isContestCreator(contest, currentUser) ||
                userService.getUserPermission(currentUser) == Teacher.ADMIN) {
            return "success";
        }
        return "negative";
    }

    @GetMapping("/background/{cid:[0-9]+}")
    public Contest getUpdateContestInfo(@PathVariable("cid") Long cid,
                                        @SessionAttribute User currentUser) {
        Contest contest = contestService.getContestById(cid);
        if (!isContestCreator(contest, currentUser)) {
            throw new ForbiddenException();
        }
        contest.setCreator(null);
        contest.setProblems(null);
        contest.setSolutions(null);
        contest.setTeam(null);
        contest.setContestComments(null);
        return contest;
    }

    @PostMapping("/background/{cid:[0-9]+}")
    public String updateContest(@PathVariable("cid") Long cid,
                                @SessionAttribute User currentUser,
                                @RequestBody EditContest editContest) {
        Contest contest = contestService.getContestById(cid);
        if (!isContestCreator(contest, currentUser) && userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Permission denied!");
        }
        contest.setTitle(editContest.getTitle());
        contest.setDescription(editContest.getDescription());
        if (!contest.getPrivilege().equals(Contest.TEAM)) {
            if (!editContest.getPrivilege().equals(Contest.TEAM)) {
                contest.setPrivilege(editContest.getPrivilege());
            }
        }
        contest.setStartAndEndTime(editContest.getStartTime(), editContest.getLength());
        if (contest.getPrivilege().equals(Contest.PRIVATE)) {
            contest.setPassword(editContest.getPassword());
        }
        contestService.saveContest(contest);
        return "success";
    }

    @GetMapping("/{cid:[0-9]+}")
    public Contest getContestDetail(@PathVariable("cid") Long cid,
                                    @RequestParam(value = "password", defaultValue = "") String password) {
        Contest c = contestService.getContestById(cid, false);
        if (c == null)
            throw new NotFoundException();
        Contest scontest = (Contest) session.getAttribute("contest" + c.getId());
        if (scontest == null || scontest.getId() != c.getId() || !c.isStarted()) {
            if (!c.isStarted() ||
                    (c.getPassword() != null && c.getPassword().length() > 0 &&
                            c.getPrivilege().equals("private") &&
                            !c.getPassword().equals(password))) {
                c.clearLazyRoles();
                c.setProblems(null);
                c.setSolutions(null);
                c.setContestComments(null);
                c.setCreator(null);
                c.setFreezeRank(null);
                c.setCreateTime(null);
                c.setPassword("password");
                return c;
            } else {
                session.setAttribute("contest" + c.getId(), c);
            }
        }
        try {
            c = contestService.getContestById(cid, true);
            c.setSolutions(null);
            c.setCreator(null);
            c.setCreateTime(null);
            c.setPassword(null);
            c.setFreezeRank(null);
            c.setCreateTime(null);
            c.setContestComments(null);
            c.setTeam(null);
            for (ContestProblem cp : c.getProblems()) {
                Problem p = cp.getProblem();
                p.setId(null);
                p.setAccepted(null);
                p.setSubmitted(null);
                p.setAccepted(null);
                p.setTags(null);
                p.setTitle(null);
                p.setScore(null);
                p.setSource(null);
            }
            c.getProblems().sort((a, b) -> (int) (a.getTempId() - b.getTempId()));
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return c;
    }

    @PostMapping("/submit/{pid:[0-9]+}/{cid:[0-9]+}")
    public Result submitProblemInContest(@PathVariable("pid") Long pid,
                                         @PathVariable("cid") Long cid,
                                         HttpServletRequest request,
                                         @RequestBody ProblemController.SubmitCodeObject submitCodeObject) {
        log.info("Submit:" + Date.from(Instant.now()));
        String source = submitCodeObject.getSource();
        boolean share = submitCodeObject.isShare();
        String language = submitCodeObject.getLanguage();
        String _temp = ProblemController.checkSubmitFrequncy(session, source);
        if (_temp != null)
            new Result(403, _temp);
        @NotNull User user;
        try {
            user = (User) session.getAttribute("currentUser");
            if (userService.getUserById(user.getId()) == null) {// user doesn't login
                throw new NeedLoginException();
            }
        } catch (Exception e) {
            throw new NeedLoginException();
        }
        try {
            Contest contest = contestService.getContestById(cid);
            Contest scontest = (Contest) session.getAttribute("contest" + cid);
            if (scontest == null || scontest.getId() != contest.getId()) {
                return new Result(403, "Need attendance!");
            }
            if (contest.isEnded() || !contest.isStarted()) {
                return new Result(403, "The contest is not Running!");
            }
            ContestProblem cproblem = contestProblemRepository.findByContestAndTempId(contest, pid).orElse(null);
            if (cproblem == null) {
                return new Result(404, "Problem Not Exist");
            }
            Problem problem = cproblem.getProblem();
            Solution solution = new Solution(user, problem, language, source, request.getRemoteAddr(), share);
            solution.setContest(contest);
            solution = solutionService.insertSolution(solution);
            assert solution.getContest() != null;
            judgeService.submitCode(solution);
            return RestfulResult.successResult();
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    @PostMapping("/comments/post/{cid:[0-9]+}")
    public String postComments(@PathVariable(value = "cid") Long cid, @RequestBody CommentPost commentPost) {
        try {
            if (commentPost.replyText.length() < 4) return "too short";
            User user = (User) session.getAttribute("currentUser");
            if (user == null) return "need login";
            ContestComment father = contestService.getFatherComment(commentPost.getReplyId());
            @NotNull Contest contest = contestService.getContestById(cid);
            if (!contest.isStarted() || contest.isEnded())
                return "contest is not running";
            ContestComment contestComment = new ContestComment(user, commentPost.replyText, father, contest);
            contestService.postComment(contestComment);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "failed";
    }

    @GetMapping("/comments/{cid:[0-9]+}")
    public List<ContestComment> getCommentsOfContest(@PathVariable Long cid) {
        try {
            @NotNull Contest contest = contestService.getContestById(cid, false);
            if (!contest.isStarted())
                throw new NotFoundException();
            List<ContestComment> contestComments = contestService.getCommentsOfContest(contest);
            contestComments.forEach(c -> c.getUser().hideInfo());
            return contestComments;
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    @GetMapping("/status/{cid:[0-9]+}")
    public Page<Solution> getUserSolutions(@PathVariable("cid") Long cid,
                                           @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            @NotNull Contest contest = contestService.getContestById(cid, true);
            if (!contest.isStarted())
                throw new NotFoundException();
            @NotNull User user = (User) session.getAttribute("currentUser");
//            User user = userService.getUserById(5l); // test
            @NotNull Page<Solution> solutions = solutionService.getSolutionsOfUserInContest(page, PAGE_SIZE, user, contest);
            Map<Long, ContestProblem> cpmap = new HashMap<>();
            for (ContestProblem cp : contest.getProblems()) {
                cpmap.put(cp.getProblem().getId(), cp);
            }
            for (Solution s : solutions.getContent()) {
                s.setSource(null);
                s.setIp(null);
                s.getUser().setEmail(null);
                s.getUser().setPassword(null);
                s.getUser().setIntro(null);
                s.getUser().setUserProfile(null);
                Problem tp = Problem.jsonReturnProblemFactory();
                tp.setId(cpmap.get(s.getProblem().getId()).getTempId());
                s.setProblem(tp);
                s.setContest(null);
            }
            return solutions;
        } catch (Exception e) {
        }
        throw new NotFoundException();
    }

    @GetMapping("/ranklist/{cid:[0-9]+}")
    @Cacheable(value = "contestRank", key = "#cid")
    public Map<String, Object> getRankOfContest(@PathVariable Long cid) {
        try {
            @NotNull Contest contest = contestService.getContestById(cid, true);
            contest.setTeam(null);
            @NotNull Rank rank = new Rank(contest);
            @NotNull List<Solution> solutions = solutionService.getSolutionsInContest(contest);
            for (int i = solutions.size() - 1; i >= 0; i--) {
                rank.update(solutions.get(i));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("problemsNumber", rank.getProblemsNumber());
            result.put("rows", rank.getRows());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new NotFoundException();
    }

    @PostMapping("/create")
    public String insertContestAction(@RequestBody CreateContest postContest
            , @SessionAttribute User currentUser) {
        try {
            if (currentUser == null)
                throw new NeedLoginException();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime localDateTime = LocalDateTime.parse(postContest.getStartTime(), dtf);
            Instant startTime = Instant.from(localDateTime.atZone(ZoneId.systemDefault()));
            Instant endTime = startTime.plusSeconds(60 * postContest.getLength());
            List<ContestProblem> contestProblems = new ArrayList<>();
            long cnt = 1L;
            for (CreateContest.CreateProblem cp : postContest.getProblems()) {
                Problem p = problemService.getProblemById(cp.getId());
                if (p == null)
                    return "problem error";
                contestProblems.add(new ContestProblem(p, cp.getTempTitle(), cnt++));
            }
            Contest contest = new Contest(postContest.getTitle(),
                    postContest.getDescription(),
                    postContest.getPrivilege(),
                    postContest.getPassword(),
                    startTime, endTime, Instant.now());
            contest.setCreator(currentUser);
            if (postContest.getPrivilege().equals(Contest.TEAM)) {
                Team team = teamService.getTeamById(postContest.getTid());
                if (team == null)
                    throw new NotFoundException();
                contest.setTeam(team);
            } else {
                contest.setTeam(null);
            }
            contest.setSolutions(null);
            contest = contestService.saveContest(contest);
            for (ContestProblem cp : contestProblems) {
                cp.setContest(contest);
                contestProblemRepository.save(cp);
            }
            return "success";
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return "failed";
    }

    @Data
    public static class CommentPost {
        String replyText = "";
        Long replyId = 0L;

        public CommentPost() {
        }

        public Long getReplyId() {
            return replyId == null ? 0L : replyId;
        }
    }

    @Data
    static class EditContest {
        String title;
        String description;
        String privilege;
        String password;
        String startTime;
        Long length;

        public EditContest() {
        }
    }

    @Data
    static class CreateContest {
        private String title;
        private String description;
        private String privilege;
        private String password;
        private String startTime;
        private Long length;
        private Long tid;
        private ArrayList<CreateProblem> problems;

        public CreateContest() {
        }

        @Data
        static class CreateProblem {
            private Long id;
            private String tempTitle;
            private String name;

            public CreateProblem() {
            }
        }
    }
  /*  @GetMapping("/rejudge/{cid}")
    public String Rejudge(@PathVariable Long cid) {
        Contest contest = contestService.getContestById(cid);
        RejudgeThread rejudgeThread = new RejudgeThread();
        List<Solution> solutions = solutionService.getSolutionsInContest(contest);
        if (!contest.getPattern().equals("acm")) {
            solutions.sort((o1, o2) -> (int) (o2.getId() - o1.getId()));
        }
        rejudgeThread.solutions = solutions;
        rejudgeThread.run();
        return "Running";
    }*/
}