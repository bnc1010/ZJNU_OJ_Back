package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.common.exception.NeedLoginException;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolationException;
import java.time.Instant;

@RestController
public class UserController {
    private final UserService userService;
    private final HttpSession session;

    public UserController(UserService userService, HttpSession session) {
        this.userService = userService;
        this.session = session;
    }

    @IgnoreSecurity
    @GetMapping("/register")
    public ModelAndView register() {
        return new ModelAndView("user/register");
    }

    @IgnoreSecurity
    @PostMapping("/register")
    public RestfulResult registerUser(@RequestBody User user) {
        System.out.println(user.toString());
        try {
            User t_user = userService.registerUser(user);
            if (t_user != null)
                return RestfulResult.successResult();
            else return new RestfulResult(400, "用户名已存在 user already existed");
        } catch (ConstraintViolationException e) {
            System.out.println(e.getMessage());
            return new RestfulResult(400, "format error 格式错误");
        }
    }

    @GetMapping("/login")
    public ModelAndView login() {
        return new ModelAndView("user/login");
    }

    @GetMapping("/logout")
    public ModelAndView logout(HttpSession session) {
        session.removeAttribute("currentUser");
        session.invalidate();
        return login();
    }

    @IgnoreSecurity
    @PostMapping("/login")
    public RestfulResult loginUser(@RequestBody User user, HttpSession session, Model m) {
        User login_user = userService.loginUser(user);
        if (login_user == null) {
            return new RestfulResult(400, "用户名或密码错误。");
        }
        session.setMaxInactiveInterval(6 * 60 * 60);
        session.setAttribute("currentUser", login_user);
        session.setAttribute("loginTime", Instant.now());
        return RestfulResult.successResult();
    }

    @GetMapping("/user/session")
    public RestfulResult getSession() {
        User user = (User) session.getAttribute("currentUser");
        int pri = userService.getUserPermission(user);
        if (user != null)
            return new RestfulResult(200, pri + "", user.hideInfo());
        throw new NeedLoginException();
    }

    @GetMapping("/permission")
    public String getUserPermission(@SessionAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new NotFoundException();
        }
        int p = (userService.getUserPermission(currentUser));
        if (p == Teacher.TEACHER) {
            return "teacher";
        } else if (p == Teacher.ADMIN) {
            return "admin";
        } else {
            return "user";
        }

    }
}
