package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.Team;
import cn.edu.zjnu.acm.entity.oj.TeamApply;
import cn.edu.zjnu.acm.entity.oj.Teammate;
import cn.edu.zjnu.acm.common.exception.ForbiddenException;
import cn.edu.zjnu.acm.common.exception.NeedLoginException;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.service.ContestService;
import cn.edu.zjnu.acm.service.TeamService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;
    private final UserService userService;
    private final ContestService contestService;
    private final TokenManager tokenManager;
    private final HttpSession session;

    public TeamController(HttpSession session, TokenManager tokenManager, TeamService teamService, UserService userService, ContestService contestService) {
        this.teamService = teamService;
        this.userService = userService;
        this.contestService = contestService;
        this.tokenManager = tokenManager;
        this.session = session;
    }

    @GetMapping("")
    public RestfulResult teamPage(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "pagesize", defaultValue = "20") int pagesize) {
        page = Math.max(0, page);
        Page<Team> return_page = null;
        try{
            return_page = teamService.getAll(page, pagesize);
        }
        catch (Exception e){
            log.info("/api/team error");
            return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        for (Team t : return_page.getContent()) {
            t.clearLazyRoles();
            t = teamService.fillTeamTeammate(t);
            t.hideInfo();
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, return_page);
    }

    @GetMapping("/myteams")
    public RestfulResult getMyTeam(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "pagesize", defaultValue = "20") int pagesize, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        Map res = null;
        try{
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            User user = userService.getUserById(tokenModel.getUserId());
            res = teamService.teamsOfUser(user, page, pagesize);
        }
        catch (Exception e){
            log.info("/api/team/myteam error");
            return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, res);
    }

    private int isUserPermitted(Team team , Integer require_level, User user) {
        if (team == null)
            return 404;
        Teammate teammate = teamService.getUserInTeam(user, team);
        if (teammate.getLevel() > require_level) {
            return 403;
        }
        return 200;
    }

    @GetMapping("/showapply/{teamid:[0-9]+}")
    public RestfulResult showApply(@PathVariable(value = "teamid") Long teamid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Team team = teamService.getTeamById(teamid);
        int status = isUserPermitted(team, Teammate.MANAGER, user);
        switch (status){
            case 404: return new RestfulResult(StatusCode.NOT_FOUND, "比赛不存在");
            case 403: return new RestfulResult(StatusCode.NO_PRIVILEGE, "没有权限");
        }
        List<TeamApply> teamApplies = null;
        try{
            teamApplies = teamService.getAllApplies(team);
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }

        for (TeamApply t : teamApplies) {
            t.setTeam(null);
            t.getUser().hideInfo();
        }
        teamApplies.sort((o1, o2) -> (int) (o1.getId() - o2.getId()) * -1);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, teamApplies);
    }

    @DeleteMapping("/delete/teammate/{id:[0-9]+}")
    public RestfulResult deleteTeammate(@PathVariable(value = "id") Long id, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Teammate teammate = teamService.getTeammateById(id);
        if (teammate == null) {
            throw new NotFoundException();
        }
        int status = isUserPermitted(teammate.getTeam(), Teammate.MANAGER, user);
        switch (status){
            case 404: return new RestfulResult(StatusCode.NOT_FOUND, "比赛不存在");
            case 403: return new RestfulResult(StatusCode.NO_PRIVILEGE, "没有权限");
        }
        Teammate selfTeammate = teamService.getUserInTeam(user, teammate.getTeam());
        if (selfTeammate.getLevel() >= teammate.getLevel())
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "权限不足");
        if (teammate.getUser().getId() == user.getId())
            return new RestfulResult(StatusCode.REQUEST_ERROR, "不能移除自己");
        teamService.deleteTeammate(teammate);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    private int updateTeammateLevel(Long tid, Integer level, User user) {
        Teammate teammate = teamService.getTeammateById(tid);
        if (teammate == null) {
            return 404;
        }
        if (isUserPermitted(teammate.getTeam(), Teammate.MASTER, user) == 403) {
            return 403;
        }
        teammate.setLevel(level);
        teamService.updateTeammate(teammate);
        return 200;
    }

    @PostMapping("/add/manager/{id:[0-9]+}")
    public RestfulResult addManager(@PathVariable(value = "id") Long tid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        int status = updateTeammateLevel(tid, Teammate.MANAGER, user);
        if (status == 403){
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "权限不足");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @PostMapping("/remove/manager/{id:[0-9]+}")
    public RestfulResult removeManager(@PathVariable(value = "id") Long tid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        int status = updateTeammateLevel(tid, Teammate.MEMBER, user);
        if (status == 403){
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "权限不足");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @PostMapping("/apply/{teamid:[0-9]+}")
    public RestfulResult applyTeam(@PathVariable(value = "teamid") Long tid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Team team = teamService.getTeamById(tid);
        if (team == null) {
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "队伍不存在");
        }
        if (team.getAttend().equals(Team.PRIVATE)) {
            return new RestfulResult(StatusCode.REQUEST_ERROR, "private队伍无法申请");
        }
        if (teamService.isUserInTeam(user, team) || teamService.isUserHasApplied(user, team))
            return new RestfulResult(StatusCode.REQUEST_ERROR, "已在队伍中或已申请");
        TeamApply teamApply = new TeamApply(user, team);
        teamApply = teamService.applyTeam(teamApply);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    private TeamApply checkTeamApplyById(Long applyid, User user) {
        TeamApply teamApply = teamService.getTeamApplyById(applyid);
        if (teamApply == null)
            throw new NotFoundException();
        if (isUserPermitted(teamApply.getTeam(), Teammate.MANAGER, user) == 403)
            throw new ForbiddenException();
        return teamApply;
    }

    @PostMapping("/apply/approve/{applyid:[0-9]+}")
    public RestfulResult applyApproveTeam(@PathVariable(value = "applyid") Long applyid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        TeamApply teamApply = null;
        try{
            teamApply = checkTeamApplyById(applyid, user);
        }
        catch (NotFoundException e){
            return new RestfulResult(StatusCode.NOT_FOUND, "用户不存在");
        }
        catch (ForbiddenException e){
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "Forbidden");
        }
        try{
            teamService.resolveApply(teamApply, true);
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @PostMapping("/apply/reject/{applyid:[0-9]+}")
    public RestfulResult applyRejectTeam(@PathVariable(value = "applyid") Long applyid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        TeamApply teamApply = null;
        try{
            teamApply = checkTeamApplyById(applyid, user);
        }
        catch (NotFoundException e){
            return new RestfulResult(StatusCode.NOT_FOUND, "用户不存在");
        }
        catch (ForbiddenException e){
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "Forbidden");
        }
        try{
            teamService.resolveApply(teamApply, false);
        }
        catch (Exception e){
            return new RestfulResult(StatusCode.HTTP_FAILURE, "system error");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @GetMapping("/{teamid:[0-9]+}")
    public RestfulResult teamIndex(@PathVariable(value = "teamid") Long teamid, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Team team = teamService.getTeamById(teamid);
        if (team == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "队伍不存在");
        if (!teamService.isUserInTeam(user, team))
            return new RestfulResult(StatusCode.NOT_FOUND, "not in the team");
        team.setContests(contestService.contestsOfTeam(team));
        team = teamService.fillTeamTeammate(team);
        team.hideInfo();
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, team);
    }

    private String generateCode(int number) {
        int offset = (int) ((Math.random() * 10) % 10);
        StringBuffer sub = new StringBuffer();
        sub.append((char) (offset + 65));
        for (int i = 1; i < 18; i++) {
            char c = (char) (Math.random() * 26 + 65);
            sub.append(c);
        }
        String str = String.format("%06d", number);
        for (int i = 1; i <= 6; i++)
            sub.setCharAt(offset + i, (char) (str.charAt(i - 1) + 17 + offset));
        return String.valueOf(sub);
    }

    private int decode(String s) {
        int offset = s.charAt(0) - 65;
        int result = 0;
        for (int i = 1; i <= 6; i++) {
            result *= 10;
            int n = s.charAt(offset + i) - 65 - offset;
            result += n;
        }
        return result;
    }

    @GetMapping("/invite/{teamid:[0-9]+}")
    public RestfulResult getInviteLink(@PathVariable(value = "teamid") Long id) {
        Team team = teamService.getTeamById(id);
        if (team == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "队伍不存在");
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, generateCode(id.intValue())) ;
    }

    @GetMapping("/invite/{code:[A-Z]{18}}")
    public RestfulResult doInviteLink(@PathVariable(value = "code") String code, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Long tid = (long) decode(code);
        Team team = teamService.getTeamById(tid);
        if (team == null)
            return new RestfulResult(StatusCode.NOT_FOUND, "邀请码无效");
        teamService.addTeammate(user, team);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @GetMapping("/update/attend/{teamid:[0-9]+}")
    public RestfulResult updateTeamAttendStrategy(@PathVariable(value = "teamid") Long teamid,
                                           @RequestParam(value = "attend") String attend,
                                           HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Team team = teamService.getTeamById(teamid);
        int status = isUserPermitted(team, Teammate.MASTER, user);
        if (status == 200) {
            try {
                assert attend.equals(Team.PRIVATE) || attend.equals(Team.PUBLIC);
                teamService.updateTeamAttend(attend, team);
            } catch (AssertionError | NullPointerException ne) {
                return new RestfulResult(StatusCode.REQUEST_ERROR, "request error");
            }
        }
        else if (status == 403){
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "no privilege");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @PostMapping("/create")
    public RestfulResult createTeam(@Validated @RequestBody Team team, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        if (userService.getUserPermission(tokenModel.getPermissionCode(), "a5") == -1) {
            return new RestfulResult(StatusCode.NO_PRIVILEGE, "permission denied");
        }
        if (teamService.isTeamNameExist(team.getName())) {
            return new RestfulResult(StatusCode.REQUEST_ERROR, "name existed!");
        }
        if (teamService.checkUserCreateTeamLimit(30, user)) {
            return new RestfulResult(StatusCode.REQUEST_ERROR, "you have created too many teams");
        }
        team.setCreator(user);
        team.setTeammates(new ArrayList<Teammate>());
        team.setContests(new ArrayList<Contest>());
        team.setCreateTime(Instant.now());
        team = teamService.addTeam(team);
        teamService.addTeammate(user, team, Teammate.MASTER);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }

    @GetMapping("/leave/{teamid:[0-9]+}")
    public RestfulResult leaveTeam(@PathVariable("teamid") Long teamId, HttpServletRequest request) {
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
        User user = userService.getUserById(tokenModel.getUserId());
        Team team = teamService.getTeamById(teamId);
        if (!teamService.isUserInTeam(user, team)) {
            return new RestfulResult(StatusCode.REQUEST_ERROR, "user not in this team");
        }
        Teammate teammate = teamService.getUserInTeam(user, team);
        if (teammate.getLevel().equals(Teammate.MASTER)) {
            teamService.deleteTeam(team);
        } else {
            teamService.deleteTeammate(teammate);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS);
    }
}