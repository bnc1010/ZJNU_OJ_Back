package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.ve.ProblemVO;
import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.config.GlobalStatus;
import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.ContestProblem;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Tag;
import cn.edu.zjnu.acm.common.exception.ForbiddenException;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.contest.ContestProblemRepository;
import cn.edu.zjnu.acm.repo.problem.AnalysisRepository;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import cn.edu.zjnu.acm.repo.problem.SolutionRepository;
import cn.edu.zjnu.acm.repo.problem.TagRepository;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.service.ContestService;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.service.SolutionService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Api(value = "API - AdminController", description = "管理api")
@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api/ojadmin")
public class AdminController {
    public static final int PAGE_SIZE = 50;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(StatusCode.HTTP_SUCCESS);
    private final UserProblemRepository userProblemRepository;
    private final ProblemService problemService;
    private final ContestService contestService;
    private final UserService userService;
    private final HttpSession session;
    private final ProblemRepository problemRepository;
    private final Config config;
    private final SolutionService solutionService;
    private final ContestProblemRepository contestProblemRepository;
    private final SolutionRepository solutionRepository;
    private final UserProfileRepository userProfileRepository;
    private final AnalysisRepository analysisRepository;
    private final TeacherRepository teacherRepository;
    private final TagRepository tagRepository;

    public AdminController(UserProblemRepository userProblemRepository, ProblemService problemService, ContestService contestService, UserService userService, HttpSession session, Config config, SolutionService solutionService, ProblemRepository problemRepository, ContestProblemRepository contestProblemRepository, SolutionRepository solutionRepository, UserProfileRepository userProfileRepository, AnalysisRepository analysisRepository, TeacherRepository teacherRepository, TagRepository tagRepository) {
        this.userProblemRepository = userProblemRepository;
        this.problemService = problemService;
        this.contestService = contestService;
        this.userService = userService;
        this.session = session;
        this.config = config;
        this.solutionService = solutionService;
        this.problemRepository = problemRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.solutionRepository = solutionRepository;
        this.userProfileRepository = userProfileRepository;
        this.analysisRepository = analysisRepository;
        this.teacherRepository = teacherRepository;
        this.tagRepository = tagRepository;
    }


    @ApiOperation(value = "系统设置", notes = "系统设置", produces = "application/json")
    @PostMapping("/config")
    public String updateConfig(@RequestBody UpdateConfig updateConfig) {
        log.info(updateConfig.toString());
        config.setLeastScoreToSeeOthersCode(updateConfig.getLeastScoreToSeeOthersCode());
        config.setLeastScoreToPostBlog(updateConfig.getLeastScoreToPostBlog());
        config.setJudgerhost(updateConfig.getJudgerhost());
        config.setC(updateConfig.getC());
        config.setCpp(updateConfig.getCpp());
        config.setJava(updateConfig.getJava());
        config.setPython2(updateConfig.getPython2());
        config.setPython3(updateConfig.getPython3());
        config.setGo(updateConfig.getGo());
        config.setNotice(updateConfig.getNotice());
        return "success";
    }


    @ApiOperation(value = "题目管理", notes = "题目管理", produces = "application/json")
    @GetMapping("/problem")
    public RestfulResult getAllProblems(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                        @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<Problem> problemPage;
        problemPage = problemService.getAllProblems(page, pagesize, search);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problemPage);
    }

    @ApiOperation(value = "比赛管理", notes = "比赛管理", produces = "application/json")
    @GetMapping("/contest")
    public RestfulResult getAllContest(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                       @RequestParam(value = "search", defaultValue = "") String search) {
        Page<Contest> contestPage = contestService.getContestPage(page, pagesize, search);
        contestPage.getContent().forEach(contest -> {
            contest.getCreator().hideInfo();
            contest.clearLazyRoles();
        });
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", contestPage);
    }

    @PostMapping("/problem/insert")
    public RestfulResult addProblem(@RequestBody ProblemVO problem) {
        if (problemService.isProblemRepeated(problem.getTitle())) {
            return new RestfulResult(StatusCode.HTTP_FAILURE, "Problem name already existed!");
        }
        Problem p = new Problem(problem.getTitle(), problem.getDescription(),
                problem.getInput(), problem.getOutput(), problem.getSampleInput(),
                problem.getSampleOutput(), problem.getHint(), problem.getSource(),
                problem.getTime(), problem.getMemory(), problem.getScore());
        p.setActive(problem.getActive());
        p.setTags(problemService.convertString2Tag(problem.getTags()));
        problemService.insertNewProblem(p);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
    }

    @ApiOperation(value = "更新题目", notes = "更新题目", produces = "application/json")
    @PostMapping("/problem/edit/{pid:[0-9]+}")
    public RestfulResult updateProblem(@RequestBody ProblemVO problem, @PathVariable("pid") Long pid) {
        Problem p = problemService.getProblemById(pid);
        if (null == p) {
            return new RestfulResult(StatusCode.HTTP_FAILURE, "Problem not existed!");
        }
        p.setTitle(problem.getTitle());
        p.setDescription(problem.getDescription());
        p.setInput(problem.getInput());
        p.setOutput(problem.getOutput());
        p.setSampleInput(problem.getSampleInput());
        p.setSampleOutput(problem.getSampleOutput());
        p.setHint(problem.getHint());
        p.setSource(problem.getSource());
        p.setTimeLimit(problem.getTime());
        p.setMemoryLimit(problem.getMemory());
        p.setScore(problem.getScore());
        p.setActive(problem.getActive());
        p.setTags(problemService.convertString2Tag(problem.getTags()));
        problemService.insertNewProblem(p);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
    }

    @ApiOperation(value = "显示题目", notes = "显示题目", produces = "application/json")
    @GetMapping("/problem/{id:[0-9]+}")
    public RestfulResult showProblem(@PathVariable Long id) {
        Problem problem = problemService.getProblemById(id);
        if (problem == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "Problem not existed!", null);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problem);
    }

//    @ApiOperation(value = "删除题目", notes = "删除题目", produces = "application/json")
//    @PostMapping("/problem/delete/  {id:[0-9]+}")
//    @Transactional
//    public RestfulResult deleteProblem(@SessionAttribute User currentUser, @PathVariable Long id) {
//        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
//            throw new ForbiddenException("Only Administrator can access");
//        }
//        Problem problem = problemRepository.findById(id).orElse(null);
//        if (problem == null) {
//            throw new NotFoundException();
//        }
//        solutionRepository.deleteAllByProblem(problem);
//        analysisRepository.deleteAllByProblem(problem);
//        userProblemRepository.deleteAllByProblem(problem);
//        contestProblemRepository.deleteAllByProblem(problem);
//        problemRepository.delete(problem);
//        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", null);
//    }

    @GetMapping("/correctData")
    public String calculateData() {
        try {
            User user = (User) session.getAttribute("currentUser");
            log.info("calculating data by user:" + user.getUsername());
            Thread main = new Thread(() -> {
                Thread threadProblem = new Thread(this::calcProblem);
                Thread threadContest = new Thread(this::calcContest);
                Thread threadUser = new Thread(this::calcUser);
                threadContest.start();
                threadProblem.start();
                threadUser.start();
                try {
                    threadContest.join();
                    threadProblem.join();
                    threadUser.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    log.info("calculating finished");
                    GlobalStatus.maintaining = false;
                }
            });
            GlobalStatus.maintaining = true;
            main.start();
            return "将持续一段时间, it will cost a long long time...";
        } catch (Exception e) {
            log.info("exception catched");
        }
        return "failed";
    }

    @Transactional
    void calcUser() {
        log.info("calculating on user");
        List<User> userList = userService.userList();
        int cnt = 0;
        for (User u : userList) {
            Future f = threadPool.submit(() -> {
                userProfileRepository.setUserSubmitted(u.getUserProfile().getId(), solutionRepository.countAllByUser(u).intValue());
                userProfileRepository.setUserAccepted(u.getUserProfile().getId(), userProblemRepository.countAllByUser(u).intValue());
                userProfileRepository.setUserScore(u.getUserProfile().getId(), userProblemRepository.calculateUserScore(u.getId()).intValue());
            });
            if (cnt == userList.size() - 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++cnt;
        }
        log.info("calculating on user finished");
    }

    @Transactional
    void calcProblem() {
        log.info("calculating on problem");
        List<Problem> problemList = problemService.getProblemList();
        int cnt = 0;
        for (Problem p : problemList) {
            Future f = threadPool.submit(() -> {
                problemRepository.setSubmittedNumber(p.getId(), solutionService.countOfProblem(p).intValue());
                problemRepository.setAcceptedNumber(p.getId(), userProblemRepository.countAllByProblem(p).intValue());
            });
            if (cnt == problemList.size() - 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++cnt;
        }
        log.info("calculating on problem finished");
    }

    @Transactional
    void calcContest() {
        log.info("calculating on contest");
        List<Contest> contestList = contestService.getContestList();
        int cnt = 0;
        for (Contest c : contestList) {
            Future f = threadPool.submit(() -> {
                List<ContestProblem> contestProblemList = contestProblemRepository.findAllByContest(c);
                for (ContestProblem cp : contestProblemList) {
                    cp.setSubmitted(solutionService.countOfProblemContest(cp.getProblem(), c).intValue());
                    cp.setAccepted(solutionService.countAcOfProblemContest(cp.getProblem(), c).intValue());
                    contestProblemRepository.save(cp);
                }
            });
            if (cnt == contestList.size() - 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++cnt;
        }
        log.info("calculating on contest finished");
    }

    @GetMapping("/maintain")
    public String maintainSwitch() {
        GlobalStatus.teacherOnly ^= true;
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            throw new ForbiddenException();
        }
        log.info(user.getId() + "set status: teacherOnly to " + GlobalStatus.teacherOnly);
        return GlobalStatus.teacherOnly ? "maintaining now" : "not maintaining now";
    }

    @GetMapping("/tag")
    public RestfulResult getAllTags() {
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, problemService.getAllTags());
    }

    @PostMapping("/tag/add")
    public RestfulResult addTag(@RequestBody Map<String, String> tagmap) {
        String tagname = tagmap.getOrDefault("tagname", "");
        if (tagname.length() == 0) {
            return new RestfulResult(400, "need input tagname");
        }
        Tag tag = tagRepository.findByName(tagname).orElse(null);
        if (tag == null) {
            tagRepository.save(new Tag(tagname));
            return RestfulResult.successResult();
        }
        return new RestfulResult(400, "already existed");
    }

    @Data
    static class UpdateConfig {
        private Integer leastScoreToPostBlog = 750;
        private Integer leastScoreToSeeOthersCode = 1000;
        private ArrayList<String> judgerhost;
        private Config.LanguageConfig c;
        private Config.LanguageConfig cpp;
        private Config.LanguageConfig java;
        private Config.LanguageConfig python2;
        private Config.LanguageConfig python3;
        private Config.LanguageConfig go;
        private String notice;

        public UpdateConfig() {
        }

        public UpdateConfig(Config config) {
            setLeastScoreToSeeOthersCode(config.getLeastScoreToSeeOthersCode());
            setLeastScoreToPostBlog(config.getLeastScoreToPostBlog());
            setJudgerhost(config.getJudgerhost());
            setC(config.getC());
            setCpp(config.getCpp());
            setJava(config.getJava());
            setPython2(config.getPython2());
            setPython3(config.getPython3());
            setGo(config.getGo());
            setNotice(config.getNotice());
        }
    }
}