package cn.edu.zjnu.acm.interceptor;

import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@ComponentScan
public class AdminCheckInterceptor implements HandlerInterceptor {
    @Autowired
    TeacherRepository teacherRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                log.info("admin page intercepted:" + user.getId() + user.getUsername());
                Teacher teacher = teacherRepository.findByUser(user).orElse(null);
                if (teacher.getPrivilege() == Teacher.ADMIN) {
                    return true;
                }
            }
            throw new ForbiddenException();
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}