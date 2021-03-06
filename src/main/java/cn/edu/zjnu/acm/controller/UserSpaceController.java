package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.common.exception.NeedLoginException;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.service.SolutionService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.PageHolder;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.UserGraph;
import com.sun.xml.bind.v2.runtime.unmarshaller.Intercepter;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserSpaceController {
    private final UserService userService;
    private final SolutionService solutionService;
    private final ProblemService problemService;
    private final TokenManager tokenManager;

    @Autowired
    RedisTemplate redisTemplate;

    public UserSpaceController(UserService userService, SolutionService solutionService, ProblemService problemService, TokenManager tokenManager) {
        this.userService = userService;
        this.solutionService = solutionService;
        this.problemService = problemService;
        this.tokenManager = tokenManager;
    }

    @GetMapping("/{uid:[0-9]+}")
    public User getUserInfo(@PathVariable(value = "uid") Long uid) {
        User user = userService.getUserById(uid);
        if (user == null) throw new NotFoundException();
        user.setPassword(null);
        return user;
    }

    @GetMapping("/pie/{uid:[0-9]+}")
    @Cacheable(value = "usergraph", key = "#uid")
    public RestfulResult getUserGraph(@PathVariable(value = "uid") Long uid) {
        User user = userService.getUserById(uid);
        if (user == null)
            throw new NotFoundException();
        UserGraph userGraph = new UserGraph();
        userGraph.getPie().setPrime(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("初级"), false));
        userGraph.getPie().setMedium(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("中级"), false));
        userGraph.getPie().setAdvance(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("高级"), false));
        userGraph.getRadar().setData_structure(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("数据结构"), true));
        userGraph.getRadar().setDynamic_programming(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("动态规划"), true));
        userGraph.getRadar().setSearch(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("搜索"), true));
        userGraph.getRadar().setGraph_theory(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("图论"), true));
        userGraph.getRadar().setProbability(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("概率论"), true));
        userGraph.getRadar().setMath(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("数论"), true));
        userGraph.getRadar().setString(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("字符串"), true));
        userGraph.getRadar().setGeometry(problemService.countSolveProblemByTag(user,
                problemService.getTagByName("计算几何"), true));
        userGraph.getRadar().init();
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", userGraph);
    }

    @PostMapping("/edit")
    public RestfulResult editUser(@RequestBody UpdateUser updateUser, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        User user  = null;
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            user = userService.getUserById(tokenModel.getUserId());
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.REQUEST_ERROR, "request error");
        }
        if (!userService.checkPassword(updateUser.getOldpassword(), user.getPassword()))
            return new RestfulResult(StatusCode.REQUEST_ERROR, "Old Password Wrong");
        if (updateUser.getPassword() != null && updateUser.getPassword().length() > 0){
            user.setPassword(updateUser.getPassword());
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            user.setPassword(encoder.encode(user.getPassword()));
        }
        user.setIntro(updateUser.getIntro());
        user.setEmail(updateUser.getEmail());
        user.setName(updateUser.getName());
        try {
            userService.updateUserInfo(user);
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
    }

    @PostMapping("/avatar")
    public RestfulResult changeAvatar(@RequestBody UpdateUser updateUser, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        User user  = null;
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            user = userService.getUserById(tokenModel.getUserId());
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.REQUEST_ERROR, "request error");
        }
        user.setAvatar(updateUser.getAvatar());
        try {
            userService.updateUserInfo(user);
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
    }

    @GetMapping("/list")
    public RestfulResult userList(@RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "pagesize", defaultValue = "0") int pagesize,HttpServletRequest request) {
        page = Math.max(0, page);
        List<User> userList = userService.userList();
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        User currentUser  = null;
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            currentUser = userService.getUserById(tokenModel.getUserId());
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.REQUEST_ERROR, "request error");
        }
        userList.sort((o1, o2) -> (o1.getUserProfile().getScore() - o2.getUserProfile().getScore()) * -1);
        RankUser cuser = null;
        List<RankUser> users = getRankUsers(userList);
        for (RankUser ru : users) {
            if (ru.getId().equals(currentUser.getId())) {
                cuser = ru;
                break;
            }
        }
        PageHolder pageHolder = new PageHolder(users, PageRequest.of(page, pagesize));
        Map<String, Object> map = new HashMap<>();
        map.put("page", pageHolder);
        map.put("userself", cuser);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", map);
    }

    public List<RankUser> getRankUsers(List<User> userList) {
        ValueOperations<String, List<RankUser>> vop = redisTemplate.opsForValue();
        if (redisTemplate.hasKey("userList")) {
            return vop.get("userList");
        }
        int rank = 1;
        List<RankUser> users = new LinkedList<>();
        for (int i = 0; i < userList.size(); i++) {
            User u = userList.get(i);
            if (i > 0 && u.getUserProfile().getScore() < userList.get(i - 1).getUserProfile().getScore()) {
                rank += 1;
            }
            users.add(new RankUser(u.getId(), u.getUsername(), u.getName(), u.getUserProfile().getScore(),
                    u.getUserProfile().getAccepted(), u.getUserProfile().getSubmitted(), rank));
        }
        vop.setIfAbsent("userList", users, Duration.ofSeconds(120));
        return users;
    }

    @GetMapping("/username/{username}")
    public void getUserByUsername(@PathVariable String username, HttpServletResponse response) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new NotFoundException();
        }
        try {
            response.sendRedirect("/user/" + user.getId());
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotFoundException();
        }
    }

    @Data
    static class UpdateUser {
        @Size(min = 1, max = 30)
        String name;
        @Size(min = 1, max = 60)
        String password;
        @Size(min = 1, max = 60)
        String oldpassword;
        @Size(max = 250)
        String intro;
        @Email
        @Size(min = 4, max = 200)
        String email;
        @Size(max = 100)
        String avatar;
    }

    @Data
    static class RankUser implements Serializable {
        private Long id;
        private String username;
        private String name;
        private Integer score;
        private Integer accepted;
        private Integer submitted;
        private Integer rank;

        public RankUser() {
        }

        public RankUser(Long id, String username, String name, Integer score, Integer accepted, Integer submitted, Integer rank) {
            this.id = id;
            this.username = username;
            this.name = name;
            this.score = score;
            this.accepted = accepted;
            this.submitted = submitted;
            this.rank = rank;
        }
    }
}