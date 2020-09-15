package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.ve.TokenVO;
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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/contest")
public class ContestController {
    private final UserService userService;
    private final ProblemService problemService;
    private final SolutionService solutionService;
    private final ContestService contestService;
    private final JudgeService judgeService;
    private final ContestProblemRepository contestProblemRepository;
    private final TeamService teamService;
    private final TokenManager tokenManager;
    private final RedisService redisService;

    public ContestController(UserService userService, ProblemService problemService, SolutionService solutionService, ContestService contestService, JudgeService judgeService, ContestProblemRepository contestProblemRepository, TeamService teamService, CommentRepository commentRepository, TokenManager tokenManager, RedisService redisService) {
        this.userService = userService;
        this.problemService = problemService;
        this.solutionService = solutionService;
        this.contestService = contestService;
        this.judgeService = judgeService;
        this.contestProblemRepository = contestProblemRepository;
        this.teamService = teamService;
        this.tokenManager = tokenManager;
        this.redisService = redisService;
    }

    @GetMapping("")
    public RestfulResult showContests(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
            @RequestParam(value = "search", defaultValue = "") String search) {
        try {
            page = Math.max(0, page);
            Page<Contest> currentPage = contestService.getContestWithoutTeam(page, pagesize, search);
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
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", currentPage);
        }
        catch(Exception e){
//            e.printStackTrace();
        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
    }

//    @GetMapping("/clone/{id:[0-9]+}")
//    public RestfulResult cloneContest(@PathVariable Long id) {
//        Contest c = contestService.getContestById(id, true);
//        if (c == null) {
//            return new RestfulResult(StatusCode.NOT_FOUND, "no contest found", null);
//        }
//        c.setSolutions(null);
//        c.setContestComments(null);
//        c.setCreator(null);
//        c.setPassword(null);
//        c.setTeam(null);
//        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", c);
//    }

    @PostMapping("/gate/{cid:[0-9]+}")
    public RestfulResult contestReady(@PathVariable("cid") Long cid, HttpServletRequest request, @RequestBody ContestVO contestVO) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            User user = userService.getUserById(tokenModel.getUserId());
            Contest contest = contestService.getContestByIdTwoType(cid, false);
            if (contest == null)
                return new RestfulResult(StatusCode.NOT_FOUND, "没有找到该比赛 not found");
            if (contest.getPrivilege().equals("private") && (contestVO.getPassword() == null || contestVO.getPassword().length()==0))
                return new RestfulResult(StatusCode.REQUEST_ERROR, "need password");
            if (contest.getPrivilege().equals("private") && (contestVO.getPassword() == null || !contestVO.getPassword().equals(contest.getPassword())))
                return new RestfulResult(StatusCode.REQUEST_ERROR, "password error");
            if (!contest.isStarted())
                return new RestfulResult(StatusCode.REQUEST_ERROR, "not started", contest.getNormalStartTime());
            if (userService.getUserPermission(tokenModel.getPermissionCode(), "a5") == -1) {
                if (contest.getPrivilege().equals(Contest.TEAM)) {
                    Team team = contest.getTeam();
                    if (teamService.isUserInTeam(user, team)) {
                        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
                    } else {
                        return new RestfulResult(StatusCode.NO_PRIVILEGE, "没有权限");
                    }
                }
            }
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
        }
        catch (Exception e){

        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
    }

    private boolean isContestCreator(Contest contest, User currentUser) {
        return contest.getCreator().getId() == currentUser.getId();
    }

//    @GetMapping("/background/access/{cid:[0-9]+}")
//    public RestfulResult accessToGetUpdateContestInfo(@PathVariable("cid") Long cid,
//                                              @RequestParam("token") String token) {
//        try{
//            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(token));
//            User user = userService.getUserById(tokenModel.getUserId());
//            if (user == null)
//                return new RestfulResult(StatusCode.NEED_LOGIN, "未登录");
//            Contest contest = contestService.getContestById(cid, true);
//            if (contest == null) {
//                return new RestfulResult(StatusCode.NOT_FOUND, "没有找到该比赛 not found");
//            }
//            if (!isContestCreator(contest, user) || userService.getUserPermission(tokenModel.getPermissionCode(), "a5") != 1) {
//                return new RestfulResult(StatusCode.NO_PRIVILEGE, "Permission denied!");
//            }
//            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
//    }



    @GetMapping("/background/{cid:[0-9]+}")
    public RestfulResult getUpdateContestInfo(@PathVariable("cid") Long cid,
                                        HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            User user = userService.getUserById(tokenModel.getUserId());
            if (user == null)
                return new RestfulResult(StatusCode.NEED_LOGIN, "未登录");
            Contest contest = contestService.getContestById(cid, true);
            if (contest == null) {
                return new RestfulResult(StatusCode.NOT_FOUND, "没有找到该比赛 not found");
            }
            if (!isContestCreator(contest, user) || userService.getUserPermission(tokenModel.getPermissionCode(), "a5") != 1) {
                return new RestfulResult(StatusCode.NO_PRIVILEGE, "Permission denied!");
            }
            contest.setCreator(null);
//            contest.setProblems(null);
            contest.setSolutions(null);
            contest.setTeam(null);
            contest.setContestComments(null);
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", contest);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
    }

    @PostMapping("/background/{cid:[0-9]+}")
    public RestfulResult updateContest(@PathVariable("cid") Long cid,
                                @RequestBody ContestVO contestVO, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            User user = userService.getUserById(tokenModel.getUserId());
            if (user == null)
                return new RestfulResult(StatusCode.NEED_LOGIN, "未登录");
            Contest contest = contestService.getContestByIdTwoType(cid, false);
            if (contest == null) {
                return new RestfulResult(StatusCode.NOT_FOUND, "没有找到该比赛 not found");
            }
            if (!isContestCreator(contest, user) && userService.getUserPermission(tokenModel.getPermissionCode(), "a5") == -1) {
                return new RestfulResult(StatusCode.NO_PRIVILEGE, "Permission denied!");
            }
            contest.setTitle(contestVO.getTitle());
            contest.setDescription(contestVO.getDescription());
            long cnt = 1L;
            List<ContestProblem> contestProblems = new ArrayList<>();
            for (ContestVO.CreateProblem cp : contestVO.getProblems()) {
                Problem p = problemService.getProblemById(cp.getId());
                if (p == null)
                    return new RestfulResult(StatusCode.NOT_FOUND, "problem error");;
                contestProblems.add(new ContestProblem(p, cp.getTempTitle(), cnt++));
            }
            contestProblemRepository.deleteByContestId(contest.getId());
            for (ContestProblem cp : contestProblems) {
                cp.setContest(contest);
                contestProblemRepository.save(cp);
            }
            if (!contest.getPrivilege().equals(Contest.TEAM)) {
                if (!contestVO.getPrivilege().equals(Contest.TEAM)) {
                    contest.setPrivilege(contestVO.getPrivilege());
                }
            }
            contest.setStartAndEndTime(contestVO.getStartTime(), contestVO.getLength());
            if (contest.getPrivilege().equals(Contest.PRIVATE)) {
                contest.setPassword(contestVO.getPassword());
            }
            contestService.saveContest(contest);
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
    }

    @PostMapping("/{cid:[0-9]+}")
    public RestfulResult getContestDetail(@PathVariable("cid") Long cid,
                                    @RequestBody ContestVO contestVO, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Contest c = contestService.getContestByIdTwoType(cid, true);
        if (c == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "contest not found");
        boolean status = contestService.checkUserInContest(user.getId(), cid);
        if (!status) {
            c.clearLazyRoles();
            c.setProblems(null);
            c.setSolutions(null);
            c.setContestComments(null);
            c.setCreator(null);
            c.setFreezeRank(null);
            c.setCreateTime(null);
            c.setPassword(null);
            c.setTeam(null);
            if (!c.isStarted() || (c.getPassword() != null && c.getPassword().length() > 0 && c.getPrivilege().equals("private") && !c.getPassword().equals(contestVO.getPassword()))){

                if (!c.isStarted()){ //比赛没开始
                    return new RestfulResult(StatusCode.REQUEST_ERROR, "contest not started", c);
                }
                else{
                    return new RestfulResult(StatusCode.REQUEST_ERROR, "password error", c);
                }
            }
            try {
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
                contestService.storeContestInRedis(c);
            } catch (Exception e) {
                return new RestfulResult(StatusCode.HTTP_FAILURE, "error");
            }
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", c);
    }

    @PostMapping("/submit/{pid:[0-9]+}/{cid:[0-9]+}")
    public RestfulResult submitProblemInContest(@PathVariable("pid") Long pid,
                                         @PathVariable("cid") Long cid,
                                         HttpServletRequest request,
                                         @RequestBody ProblemController.SubmitCodeObject submitCodeObject) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        log.info("Submit:" + Date.from(Instant.now()));
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        String source = submitCodeObject.getSource();
        boolean share = false;
        String language = submitCodeObject.getLanguage();
        String _temp = problemService.checkSubmitFrequency(tokenModel.getUserId(), source);
        if (_temp != null)
            new RestfulResult(StatusCode.NO_PRIVILEGE, _temp, null , null);
        User user;
        try {
            user = userService.getUserById(tokenModel.getUserId());
            if (user == null) {// user doesn't login
                return new RestfulResult(StatusCode.NEED_LOGIN, "need login");
            }
        } catch (Exception e) {
            return new RestfulResult(StatusCode.NEED_LOGIN, "need login");
        }
        try {
            Contest contest = contestService.getContestByIdTwoType(cid, false);
            boolean status = contestService.checkUserInContest(user.getId(), cid);
            if (!status) {
                return new RestfulResult(StatusCode.REQUEST_ERROR, "Need attendance!");
            }
            if (contest.isEnded() || !contest.isStarted()) {
                return new RestfulResult(StatusCode.REQUEST_ERROR, "The contest is not Running!", null , null);
            }
            ContestProblem cproblem = contestProblemRepository.findByContestAndTempId(contest, pid).orElse(null);
            if (cproblem == null) {
                return new RestfulResult(StatusCode.NOT_FOUND, "Problem Not Exist", null , null);
            }
            Problem problem = cproblem.getProblem();

            Solution solution = new Solution(user, problem, language, source, request.getRemoteAddr(), share);
            solution.setContest(contest);
            solution = solutionService.insertSolution(solution);
            assert solution.getContest() != null;
            judgeService.submitCode(solution);
            return RestfulResult.successResult();
        } catch (Exception e) {
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, "error");
        }
    }

    @PostMapping("/comments/post/{cid:[0-9]+}")
    public String postComments(@PathVariable(value = "cid") Long cid, @RequestBody CommentPost commentPost, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        try {
            if (commentPost.replyText.length() < 4) return "too short";
            User user = userService.getUserById(tokenModel.getUserId());
            if (user == null) return "need login";
            ContestComment father = contestService.getFatherComment(commentPost.getReplyId());
            @NotNull Contest contest = contestService.getContestByIdTwoType(cid, false);
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
                                           @RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                           HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        try {
            @NotNull Contest contest = contestService.getContestById(cid, true);
            if (!contest.isStarted())
                throw new NotFoundException();
            @NotNull User user = userService.getUserById(tokenModel.getUserId());
//            User user = userService.getUserById(5l); // test
            @NotNull Page<Solution> solutions = solutionService.getSolutionsOfUserInContest(page, pagesize, user, contest);
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
    public RestfulResult getRankOfContest(@PathVariable Long cid) {
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
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success",result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new RestfulResult(StatusCode.NOT_FOUND, "not found");
    }

    @PostMapping("/create")
    public RestfulResult insertContestAction(@RequestBody ContestVO postContest) {
        try {
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(postContest.getToken()));
            User user = userService.getUserById(tokenModel.getUserId());
            if (user == null)
                return new RestfulResult(StatusCode.NEED_LOGIN, "未登录");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime localDateTime = LocalDateTime.parse(postContest.getStartTime(), dtf);
            Instant startTime = Instant.from(localDateTime.atZone(ZoneId.systemDefault()));
            Instant endTime = startTime.plusSeconds(60 * postContest.getLength());
            List<ContestProblem> contestProblems = new ArrayList<>();
            long cnt = 1L;
            for (ContestVO.CreateProblem cp : postContest.getProblems()) {
                Problem p = problemService.getProblemById(cp.getId());
                if (p == null)
                    return new RestfulResult(StatusCode.NOT_FOUND, "problem error");;
                contestProblems.add(new ContestProblem(p, cp.getTempTitle(), cnt++));
            }
            Contest contest = new Contest(postContest.getTitle(),
                    postContest.getDescription(),
                    postContest.getPrivilege(),
                    postContest.getPassword(),
                    startTime, endTime, Instant.now());
            contest.setCreator(user);
            if (postContest.getPrivilege().equals(Contest.TEAM)) {
                Team team = teamService.getTeamById(postContest.getTid());
                if (team == null)
                    return new RestfulResult(StatusCode.NOT_FOUND, "team not found");
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
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
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
        String token;

        public EditContest() {
        }
    }

    @Data
    static class ContestVO {
        private String title;
        private String description;
        private String privilege;
        private String password;
        private String startTime;
        private Long length;
        private Long tid;
        private String token;
        private ArrayList<CreateProblem> problems;

        public ContestVO() {
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