package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Analysis;
import cn.edu.zjnu.acm.entity.oj.AnalysisComment;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.common.exception.ForbiddenException;
import cn.edu.zjnu.acm.common.exception.NeedLoginException;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.service.*;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/problems")
public class ProblemController {
    private final ProblemService problemService;
    private final UserService userService;
    private final JudgeService judgeService;
    private final SolutionService solutionService;
    private final HttpSession session;
    private final TokenManager tokenManager;


    public ProblemController(ProblemService problemService, UserProblemRepository userProblemRepository, UserService userService, JudgeService judgeService, SolutionService solutionService, HttpSession session, TokenManager tokenManager) {
        this.problemService = problemService;
        this.userService = userService;
        this.judgeService = judgeService;
        this.solutionService = solutionService;
        this.session = session;
        this.tokenManager = tokenManager;
    }


    Problem checkProblemExist(Long pid) {
        Problem problem = problemService.getProblemById(pid);
        if (problem == null) {
            throw new NotFoundException("No Problem Found");
        }
        return problem;
    }

    @IgnoreSecurity
    @GetMapping("")
    public RestfulResult showProblemList(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                         @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<Problem> problemPage = null;
        try{
            if (search != null && search.length() > 0) {
                int spl = search.lastIndexOf("$$");
                if (spl >= 0) {
                    String tags = search.substring(spl + 2);
                    search = search.substring(0, spl);
                    String[] tagNames = tags.split("\\,");
                    List<Problem> _problems = problemService.searchActiveProblem(0, 1, search, true).getContent();
                    problemPage = problemService.getByTagName(page, pagesize, Arrays.asList(tagNames), _problems);
                } else {
                    problemPage = problemService.searchActiveProblem(page, pagesize, search, false);
                }
            } else {
                problemPage = problemService.getAllActiveProblems(page, pagesize);
            }
            for (Problem p : problemPage.getContent()) {
                p.setInput(null);
                p.setOutput(null);
                p.setHint(null);
                p.setSource(null);
                p.setSampleInput(null);
                p.setSampleOutput(null);
            }
        }
        catch (Exception e){
            return  new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problemPage);
    }

    @GetMapping("/{id:[0-9]+}")
    public RestfulResult showProblem(@PathVariable Long id) {
        Problem problem = problemService.getActiveProblemById(id);
        if (problem == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "not found", null);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problem);
    }

    @GetMapping("/name/{id:[0-9]+}")
    public RestfulResult getProblemName(@PathVariable(value = "id") Long id) {
        try {
            Problem problem = (Problem) showProblem(id).getData();
            if (problem != null)
                return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problem.getTitle());
            else
                return new RestfulResult(StatusCode.NOT_FOUND, "not found", null);

        } catch (Exception e) {

        }
        return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
    }

    @PostMapping("/submit/{id:[0-9]+}")
    public RestfulResult submitProblem(@PathVariable("id") Long id,
                                @RequestBody SubmitCodeObject submitCodeObject,
                                HttpServletRequest request) {
        try{
            String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            String source = submitCodeObject.getSource();
            boolean share = submitCodeObject.isShare();
            String language = submitCodeObject.getLanguage();
            String _temp = problemService.checkSubmitFrequency(tokenModel.getUserId(), source);
            if (_temp != null)
                return new RestfulResult(StatusCode.NO_PRIVILEGE, _temp, null , null);
            User user = userService.getUserById(tokenModel.getUserId());
            if (user == null) {
                return new RestfulResult(StatusCode.NEED_LOGIN, "need login");
            }
            Problem problem = problemService.getActiveProblemById(id);
            if (problem == null) {
                return new RestfulResult(StatusCode.NOT_FOUND, "problem not found");
            }

        //null检验完成
        Solution solution = solutionService.insertSolution(new Solution(user, problem, language, source, request.getRemoteAddr(), share));
        if (solution == null)
            return new RestfulResult(StatusCode.HTTP_FAILURE, "submitted failed", null , null);
//            return restService.submitCode(solution) == null ? "judge failed" : "success";
            judgeService.submitCode(solution);
            return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", null , null);
        } catch (Exception e) {
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, "Internal error", null , null);
        }
    }

    @IgnoreSecurity
    @GetMapping("/tags")
    public RestfulResult showTags() {
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problemService.getAllTags());
    }

    @GetMapping("/is/accepted/{pid:[0-9]+}")
    public RestfulResult checkUserHasAc(@SessionAttribute User currentUser, @PathVariable Long pid) {
        Problem problem = checkProblemExist(pid);
        return new RestfulResult(StatusCode.HTTP_SUCCESS,
                "success",
                problemService.isUserAcProblem(currentUser, problem));
    }

    @GetMapping("/analysis/{pid:[0-9]+}")
    public RestfulResult getProblemArticle(@PathVariable Long pid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        Problem problem = checkProblemExist(pid);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        if (userService.getUserPermission(tokenModel.getPermissionCode(), "a5") == -1) {//a5是oj管理员
            if (!problemService.isUserAcProblem(user, problem)) {
                throw new ForbiddenException("Access after passing the question");
            }
        }
        List<Analysis> analyses = problemService.getAnalysisByProblem(problem);
        analyses.forEach(a -> a.getUser().hideInfo());
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", analyses);
    }

//    @PostMapping("/analysis/post/{pid:[0-9]+}")
//    public RestfulResult postAnalysis(@PathVariable Long pid,
//                                      @SessionAttribute User currentUser,
//                                      @RequestBody @Validated Analysis analysis) {
//        Problem problem = checkProblemExist(pid);
//        if (userService.getUserPermission(currentUser) == -1) {
//            if (!problemService.isUserAcProblem(currentUser, problem)) {
//                throw new ForbiddenException("Access after passing the question");
//            }
//        }
//        analysis.setUser(currentUser);
//        analysis.setComment(null);
//        analysis.setPostTime(Instant.now());
//        analysis.setProblem(problem);
//        problemService.postAnalysis(analysis);
//        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", null);
//    }
//
//    @GetMapping("/analysis/edit/{aid:[0-9]+}")
//    public RestfulResult getOneAnalysis(@PathVariable Long aid, @SessionAttribute User currentUser) {
//        Analysis analysis = problemService.getAnalysisById(aid);
//        if (analysis == null) {
//            throw new NotFoundException("Analysis not found");
//        }
//        if (userService.getUserPermission(currentUser) == -1) {
//            if (analysis.getUser().getId() != currentUser.getId()) {
//                throw new ForbiddenException("Permission denied");
//            }
//        }
//        analysis.getUser().hideInfo();
//        analysis.setProblem(null);
//        analysis.setComment(null);
//        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", analysis);
//    }
//
//    @PostMapping("/analysis/edit/{aid:[0-9]+}")
//    public RestfulResult editAnalysis(@PathVariable Long aid,
//                                      @SessionAttribute User currentUser,
//                                      @RequestBody @Validated Analysis analysis) {
//        Analysis ana = problemService.getAnalysisById(aid);
//        if (analysis == null) {
//            throw new NotFoundException("Analysis not found");
//        }
//        if (userService.getUserPermission(currentUser) == -1) {
//            if (ana.getUser().getId() != currentUser.getId()) {
//                throw new ForbiddenException("Permission denied");
//            }
//        }
//        ana.setText(analysis.getText());
//        problemService.postAnalysis(ana);
//        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", null);
//    }

//    @PostMapping("/analysis/post/comment/{aid:[0-9]+}")
//    public RestfulResult postAnalysisComment(@PathVariable Long aid,
//                                             @SessionAttribute User currentUser,
//                                             @RequestBody ContestController.CommentPost commentPost) {
//        if (commentPost.replyText.length() < 4) {
//            return new RestfulResult(400, "bad request", "too short!");
//        }
//        Analysis analysis = problemService.getAnalysisById(aid);
//        if (analysis == null) {
//            throw new NotFoundException("Analysis not found");
//        }
//        if (userService.getUserPermission(currentUser) == -1) {
//            if (!problemService.isUserAcProblem(currentUser,
//                    checkProblemExist(analysis.getProblem().getId()))) {
//                throw new ForbiddenException("Access after passing the question");
//            }
//        }
//        AnalysisComment father = problemService.getFatherComment(commentPost.getReplyId());
//        problemService.postAnalysisComment(new AnalysisComment(currentUser, commentPost.replyText, father, analysis));
//        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", null);
//    }

    public static class SubmitCodeObject {
        private String source;
        private String language;
        private String share;
        private String token;

        public SubmitCodeObject() {
        }

        public Boolean isShare() {
            return share.equals("true");
        }

        public void setShare(String share) {
            this.share = share;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }
}
