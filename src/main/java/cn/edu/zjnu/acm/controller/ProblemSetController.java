package cn.edu.zjnu.acm.controller;


import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.annotation.LogsOfUser;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.ProblemSet;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.service.*;
import cn.edu.zjnu.acm.util.RestfulResult;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Api(tags = "ProblemSetController", basePath = "/api/problems/problemset")
@RestController
@Slf4j
@CrossOrigin
@RequestMapping("/api/problems/problemset")
public class ProblemSetController {
    private final ProblemSetService problemSetService;
    private final RedisService redisService;
    private final UserService userService;

    public ProblemSetController(ProblemSetService problemSetService, RedisService redisService,
                                UserService userService) {
        this.problemSetService = problemSetService;
        this.redisService = redisService;
        this.userService = userService;
    }

    @IgnoreSecurity
    @GetMapping("")
    @LogsOfUser
    public RestfulResult showProblemSetList(@RequestParam(value = "page", defaultValue = "0") int page,
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
                    List<ProblemSet> _problemSet = problemSetService.searchActiveProblemSet(0, 1, search, true).getContent();
                    problemSetPage = problemSetService.getByTagName(page, pagesize, Arrays.asList(tagNames), _problemSet);
                } else {
                    problemSetPage = problemSetService.searchActiveProblemSet(page, pagesize, search, false);
                }
            } else {
                problemSetPage = problemSetService.getAllActiveProblemSet(page, pagesize);
            }
        }
        catch (Exception e){
            return  new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        for (ProblemSet problemSet : problemSetPage.getContent()) {
            problemSet.getCreator().setSalt(null);
            problemSet.getCreator().setPassword(null);
            problemSet.getCreator().setLevel(-1);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problemSetPage);
    }

    @GetMapping("/{id:[0-9]+}")
    @LogsOfUser
    public RestfulResult showProblem(@PathVariable Long id) {
        ProblemSet problemSet = problemSetService.getActiveProblemById(id);
        if (problemSet == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "not found", null);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", problemSet);
    }


    @GetMapping("/available")
    public RestfulResult getAvaliableProblemSet(@RequestParam("type") int type, HttpServletRequest request){
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = redisService.getToken(tk);
        User user = userService.getUserById(tokenModel.getUserId());
        List<ProblemSet> problemSets = null;
        try {
            switch (type){
                case 1:{
                    problemSets = problemSetService.getAllProblemSetByCreator(user);
                    break;
                }
                case 2:{
                    problemSets = problemSetService.getActiveProblemSetNotOfUser(user);
                    break;
                }
                case 3:{
                    problemSets = problemSetService.getAllActiveProblemSetOrCreator(user);
                    break;
                }
            }
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }
        for (ProblemSet ps : problemSets){
            ps.setCreator(null);
            ps.setTags(null);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, problemSets);
    }
}
