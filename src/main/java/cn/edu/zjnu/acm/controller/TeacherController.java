package cn.edu.zjnu.acm.controller;


import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.LogsOfAdmin;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.ve.ProblemSetVO;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.ProblemSet;
import cn.edu.zjnu.acm.entity.oj.Team;
import cn.edu.zjnu.acm.repo.problem.ProblemSetRepository;
import cn.edu.zjnu.acm.service.*;
import cn.edu.zjnu.acm.util.RestfulResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Api(value = "API - TeacherController", description = "教师api")
@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final ProblemService problemService;
    private final ContestService contestService;
    private final UserService userService;
    private final TeamService teamService;
    private final RedisService redisService;
    private final ProblemSetService problemSetService;
    private final ProblemSetRepository problemSetRepository;

    public TeacherController(ProblemService problemService, ContestService contestService, UserService userService,
                             RedisService redisService, TeamService teamService, ProblemSetService problemSetService,
                             ProblemSetRepository problemSetRepository) {

        this.problemService = problemService;
        this.contestService = contestService;
        this.userService = userService;
        this.redisService = redisService;
        this.teamService = teamService;
        this.problemSetService = problemSetService;
        this.problemSetRepository = problemSetRepository;
    }


    @ApiOperation(value = "教师端学生组", notes = "教师端学生组", produces = "application/json")
    @PostMapping("/studentGroup")
    public RestfulResult getStudentGroup(HttpServletRequest request){
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = redisService.getToken(tk);
        User user = userService.getUserById(tokenModel.getUserId());
        Page<Team> teams = null;
        try {
            teams = teamService.teamsByCreator(user, 0, 20);
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }
        for (Team t : teams.getContent()) {
            t.clearLazyRoles();
            t.hideInfo();
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, teams);
    }

    @GetMapping("/problemSet")
    @LogsOfAdmin
    public RestfulResult getProblemSetList(@RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                           @RequestParam(value = "search", defaultValue = "") String search,
                                            HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = redisService.getToken(tk);
        User user = userService.getUserById(tokenModel.getUserId());

        page = Math.max(page, 0);
        Page<ProblemSet> problemSetPage = null;
        try{
            if (search != null && search.length() > 0) {
                int spl = search.lastIndexOf("$$");
                if (spl >= 0) {
                    String tags = search.substring(spl + 2);
                    search = search.substring(0, spl);
                    String[] tagNames = tags.split("\\,");
                    List<ProblemSet> _problemSet = problemSetService.getAllProblemSet(0, 1, search, user).getContent();
                    problemSetPage = problemSetService.getByTagName(page, pagesize, Arrays.asList(tagNames), _problemSet);
                } else {
                    problemSetPage = problemSetService.getAllProblemSet(page, pagesize, search, user);
                }
            } else {
                problemSetPage = problemSetService.getAllProblemSet(page, pagesize, "", user);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }
        for (ProblemSet problemSet : problemSetPage.getContent()) {
            problemSet.getCreator().setSalt(null);
            problemSet.getCreator().setPassword(null);
            problemSet.getCreator().setLevel(-1);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, problemSetPage);
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

}
