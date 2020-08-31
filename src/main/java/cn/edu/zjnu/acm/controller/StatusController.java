package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.common.ve.TokenVO;
import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.service.SolutionService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;


@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/status")
public class StatusController {
    private final Config config;
    private final SolutionService solutionService;
    private final UserService userService;
    private final ProblemService problemService;
    private final TokenManager tokenManager;

    public StatusController(Config config, SolutionService solutionService, UserService userService, ProblemService problemService, TokenManager tokenManager) {
        this.config = config;
        this.solutionService = solutionService;
        this.userService = userService;
        this.problemService = problemService;
        this.tokenManager  = tokenManager;
    }

    public static Solution solutionFilter(Solution s) {
        if (s.getContest() != null && !s.getContest().isEnded()) {
            s.getUser().setId(0l);
            s.getUser().setName("contest user");
            s.getUser().setUsername("contest user");
            s.setLanguage("c");
            s.setCaseNumber(0);
            s.getProblem().setId(0l);
            s.setTime(0);
            s.setMemory(0);
            s.setLength(0);
            s.setShare(false);
        }
        s.setIp(null);
        s.getUser().hideInfo();
        Problem p = Problem.jsonReturnProblemFactory();
        p.setId(s.getProblem().getId());
        s.setProblem(p);
        s.setContest(null);
        return s;
    }

    @GetMapping("")
    public RestfulResult searchStatus(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                      @RequestParam(value = "pagesize", defaultValue = "20") Integer pagesize,
                                      @RequestParam(value = "user", defaultValue = "") String username,
                                      @RequestParam(value = "pid", defaultValue = "") Long pid,
                                      @RequestParam(value = "AC", defaultValue = "") String AC) throws Exception {
        try{
            if (AC.equals("true"))
                AC = "Accepted";
            else
                AC = "";
            page = Math.max(page, 0);
            boolean getAll = false;
            if (username.equals("") && pid == null && AC.equals(""))
                getAll = true;
            User user = userService.getUserByUsername(username);
            Problem problem = problemService.getActiveProblemById(pid);
            Page<Solution> page_return = getAll ?
                    solutionService.getStatus(page, pagesize) :
                    solutionService.getStatus(user, problem, AC, page, pagesize);
            page_return.getContent().forEach(s -> {
                try {
                    s.setUser(s.getUser().clone());
                } catch (CloneNotSupportedException ignored) {
                }
                s.setSource(null);
                s.setInfo(null);
                solutionFilter(s);
            });
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", page_return);
        }
        catch (Exception e){
            log.info("system turn out a mistake in api /api/status");
        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "error");
    }

    @GetMapping("/view/{id:[0-9]+}")
    public RestfulResult restfulShowSourceCode(@PathVariable(value = "id") Long id, HttpServletRequest request) {
        Solution solution = null;
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        try {
            solution = solutionService.getSolutionById(id);
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            assert solution != null;
            User user = userService.getUserById(tokenModel.getUserId());
            if (user == null) {
                solution.setInfo(null);
                solution.setSource("This Source Code Is Not Shared!");
            }
            if (userService.getUserPermission(tokenModel.getPermissionCode(), "a5") == -1 && !solution.getShare()) {
                if (user.getId() != solution.getUser().getId()) {
                    // This submit not belongs to this user.
                    solution.setSource("This Source Code Is Not Shared!");
                    solution.setInfo(null);
                } else if (user.getUserProfile().getScore() < config.getLeastScoreToSeeOthersCode() && solution.getShare()) {
                    // This submit is not shared and user doesn't have enough score
                    solution.setSource("This Source Code Is Not Shared!");
                    solution.setInfo(null);
                } else {
                    solution.setContest(null);
                }
            } else {
                solution.setContest(null);
            }
            return  new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, solutionFilter(solution));
        } catch (Exception e) {
            e.printStackTrace();
            throw new NotFoundException();
        }
    }

    @PostMapping("/share/{id:[0-9]+}")
    public RestfulResult setShare(@PathVariable("id") Long id, @RequestBody TokenVO tokenVO) {
        try {
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tokenVO.getToken()));
            if (tokenModel == null){
                throw new NotFoundException();
            }
            User user = userService.getUserById(tokenModel.getUserId());
            System.out.println(user.toString());
            if (user != null) {
                Solution solution = solutionService.getSolutionById(id);
                if (solution.getUser().getId().equals(user.getId())) {
                    solution.setShare(!solution.getShare());
                    solutionService.updateSolutionShare(solution);
                    return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, solution.getShare());
                }
            }
        } catch (Exception ignored) {
        }
        throw new NotFoundException();
    }

    @GetMapping("/user/latest/submit/{id:[0-9]+}")
    public RestfulResult userSubmitLatestHistory(@PathVariable("id") Long pid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        try {
            Problem problem = problemService.getActiveProblemById(pid);
            User user = userService.getUserById(tokenModel.getUserId());
            if (user != null && problem != null) {
                List<Solution> solutions = solutionService.getProblemSubmitOfUser(user, problem);
                solutions = solutions.subList(0, Math.min(solutions.size(), 5));
                solutions.forEach(s -> {
                    s.setUser(null);
                    s.setProblem(null);
                    s.setIp(null);
                    s.setSource(null);
                    s.setContest(null);
                });
                return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, solutions);
            }
        } catch (Exception ignored) {
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, new LinkedList<>());
    }

}
