package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.LogsOfAdmin;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.ve.ProblemSetVO;
import cn.edu.zjnu.acm.common.ve.ProblemVO;
import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.config.GlobalStatus;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.*;
import cn.edu.zjnu.acm.common.exception.ForbiddenException;
import cn.edu.zjnu.acm.repo.contest.ContestProblemRepository;
import cn.edu.zjnu.acm.repo.problem.*;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.service.*;
import cn.edu.zjnu.acm.util.RestfulResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static cn.edu.zjnu.acm.common.utils.Class2Map.objectToMap;
import static cn.edu.zjnu.acm.common.utils.YmlUpdateUtil.updateYamlFile;

@Slf4j
@Api(value = "API - AdminController", description = "管理api")
@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api/ojadmin")
public class AdminController {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(StatusCode.HTTP_SUCCESS);
    private final UserProblemRepository userProblemRepository;
    private final ProblemService problemService;
    private final ContestService contestService;
    private final UserService userService;
    private final HttpSession session;
    private final ProblemRepository problemRepository;
    private final ProblemSetService problemSetService;
    private final Config config;
    private final SolutionService solutionService;
    private final ContestProblemRepository contestProblemRepository;
    private final SolutionRepository solutionRepository;
    private final UserProfileRepository userProfileRepository;
    private final AnalysisRepository analysisRepository;
    private final TagRepository tagRepository;
    private final ProblemSetRepository problemSetRepository;
    private final RedisService redisService;

    public AdminController(UserProblemRepository userProblemRepository, ProblemService problemService,
                           ContestService contestService, UserService userService, HttpSession session,
                           Config config, SolutionService solutionService, ProblemRepository problemRepository,
                           ContestProblemRepository contestProblemRepository, SolutionRepository solutionRepository,
                           UserProfileRepository userProfileRepository, AnalysisRepository analysisRepository,
                           ProblemSetService problemSetService, TagRepository tagRepository,ProblemSetRepository problemSetRepository,
                           RedisService redisService) {
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
        this.tagRepository = tagRepository;
        this.problemSetService = problemSetService;
        this.problemSetRepository = problemSetRepository;
        this.redisService = redisService;
    }


    @GetMapping("/config")
    public RestfulResult getConfig() {
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", new UpdateConfig(config));
    }

    @ApiOperation(value = "系统设置", notes = "系统设置", produces = "application/json")
    @PostMapping("/config")
    @LogsOfAdmin
    public RestfulResult updateConfig(@RequestBody UpdateConfig updateConfig) {
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

        String src = "";
        File directory = new File("application.yml");
        if (directory.exists()){
            src = "application.yml";
        }
        else{
            directory = new File("src/main/resources/application.yml");
            if (directory.exists()){
                src = "src/main/resources/application.yml";
            }
            else {
                return new RestfulResult(StatusCode.HTTP_FAILURE, "配置文件请放在正确位置");
            }
        }
        log.info("setting: " + directory.getAbsolutePath());
        String [] keys = {"judgerhost", "c", "cpp", "go", "java", "python2", "python3",
                "least-score-to-see-others-code", "least-score-to-post-blog", "notice"};

        try{
            Object [] values = {config.getJudgerhost(), objectToMap(config.getC()), objectToMap(config.getCpp()),
                    objectToMap(config.getGo()), objectToMap(config.getJava()),objectToMap(config.getPython2()),
                    objectToMap(config.getPython3()), config.getLeastScoreToSeeOthersCode(), config.getLeastScoreToPostBlog(),
                    config.getNotice()};
            updateYamlFile(src, "onlinejudge", keys, values);
        }
        catch (IllegalAccessException e){
            e.printStackTrace();
        }

        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
    }


    @ApiOperation(value = "题目管理", notes = "题目管理", produces = "application/json")
    @GetMapping("/problem")
    @LogsOfAdmin
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
    @LogsOfAdmin
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
    @LogsOfAdmin
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
    @LogsOfAdmin
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


    @GetMapping("/problemset")
    @LogsOfAdmin
    public RestfulResult getProblemSetList(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                       @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<ProblemSet> problemSetPage = null;
        try{
            if (search != null && search.length() > 0) {
                int spl = search.lastIndexOf("$$");
                if (spl >= 0) {
                    String tags = search.substring(spl + 2);
                    search = search.substring(0, spl);
                    String[] tagNames = tags.split("\\,");
                    List<ProblemSet> _problemSet = problemSetService.getAllProblemSet(0, 1, search).getContent();
                    problemSetPage = problemSetService.getByTagName(page, pagesize, Arrays.asList(tagNames), _problemSet);
                } else {
                    problemSetPage = problemSetService.getAllProblemSet(page, pagesize, search);
                }
            } else {
                problemSetPage = problemSetService.getAllProblemSet(page, pagesize, "");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return  new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        for (ProblemSet problemSet : problemSetPage.getContent()) {
            problemSet.getCreator().setSalt(null);
            problemSet.getCreator().setPassword(null);
            problemSet.getCreator().setLevel(-1);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problemSetPage);
    }

    @PostMapping("/problemset/insert")
    @LogsOfAdmin
    public RestfulResult addProblemSet(@RequestBody ProblemSetVO problemSetVO, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = redisService.getToken(tk);
        User user = userService.getUserById(tokenModel.getUserId());
        if (problemSetService.isProblemSetRepeated(problemSetVO.getTitle())) {
            return new RestfulResult(StatusCode.HTTP_FAILURE, "ProblemSet name already existed!");
        }
        ProblemSet problemSet = null;
        try {
            problemSet = new ProblemSet();
            problemSet.setTitle(problemSetVO.getTitle());
            problemSet.setActive(problemSetVO.getActive());
            problemSet.setDescription(problemSetVO.getDescription());
            problemSet.setIsPrivate(problemSetVO.getIsPrivate());
            problemSet.setCreator(user);
            problemSet.setTags(problemService.convertString2TagReturnSet(problemSetVO.getTags()));
            problemSet.setProblems(problemSetService.getProblemArrayByIds(problemSetVO.getProblems()));
        }
        catch (Exception e){
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }

        problemSetRepository.save(problemSet);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @PostMapping("/problemset/update/{id:[0-9]+}")
    @LogsOfAdmin
    public RestfulResult editProblemSet(@PathVariable("id") Long id, @RequestBody ProblemSetVO problemSetVO){
        ProblemSet problemSet = problemSetService.getProblemSetById(id);
        if (problemSet == null){
            return new RestfulResult(StatusCode.NOT_FOUND, "题目集不存在");
        }
        if (!problemSetVO.getTitle().equals(problemSet.getTitle()) && problemSetService.isProblemSetRepeated(problemSetVO.getTitle())) {
            return new RestfulResult(StatusCode.HTTP_FAILURE, "ProblemSet name already existed!");
        }
        try{
            problemSet.setTitle(problemSetVO.getTitle());
            problemSet.setDescription(problemSetVO.getDescription());
            problemSet.setActive(problemSetVO.getActive());
            problemSet.setProblems(problemSetService.getProblemArrayByIds(problemSetVO.getProblems()));
            problemSet.setTags(problemService.convertString2TagReturnSet(problemSetVO.getTags()));
            problemSetService.insertNewProblemSet(problemSet);
        }
        catch (Exception e){
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }


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
    @LogsOfAdmin
    public RestfulResult getAllTags() {
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, problemService.getAllTags());
    }

    @PostMapping("/tag/add")
    @LogsOfAdmin
    public RestfulResult addTag(@RequestBody Map<String, String> tagmap) {
        String tagname = tagmap.getOrDefault("tagname", "");
        if (tagname.length() == 0) {
            return new RestfulResult(StatusCode.REQUEST_ERROR, "need input tagname");
        }
        Tag tag = tagRepository.findByName(tagname).orElse(null);
        if (tag == null) {
            tagRepository.save(new Tag(tagname));
            return RestfulResult.successResult();
        }
        return new RestfulResult(StatusCode.REQUEST_ERROR, "already existed");
    }

    @PostMapping("/tag/edit/{id:[0-9]+}")
    @LogsOfAdmin
    public RestfulResult editTag(@PathVariable("id") Long id, @RequestBody Tag tag) {
        Tag _tag = tagRepository.findById(id).orElse(null);
        if (_tag == null){
            return new RestfulResult(StatusCode.NOT_FOUND, "标签不存在");
        }
        if (tag.getName() == null || tag.getName().length() == 0){
            return new RestfulResult(StatusCode.REQUEST_ERROR, "need input tagname");
        }
        Tag _tagName = tagRepository.findByName(tag.getName()).orElse(null);
        if (_tagName == null || !_tagName.getId().equals(_tag.getId())){
            _tag.setName(tag.getName());
            tagRepository.save(_tag);
            return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
        }
        return new RestfulResult(StatusCode.REQUEST_ERROR, "标签名已存在");
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