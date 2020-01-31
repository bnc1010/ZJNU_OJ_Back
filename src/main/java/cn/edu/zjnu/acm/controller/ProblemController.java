package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.NotFoundException;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.entity.oj.Tag;
import cn.edu.zjnu.acm.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequestMapping("/problems")
@Controller
class ProblemViewController {
    @GetMapping
    public String problemsList() {
        return "problem/problemlist";
    }

    @GetMapping("/{id}")
    public String showproblem(@PathVariable Long id) {
        return "problem/showproblem";
    }
}

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/problems")
public class ProblemController {
    private static final int PAGE_SIZE = 30;
    @Autowired
    ProblemService problemService;
    @Autowired
    private HttpSession session;
    @Autowired
    UserService userService;
    @Autowired
    JudgeService judgeService;
    @Autowired
    SolutionService solutionService;

    public static String checkSubmitFrequncy(HttpSession session, String source) {
        if (session.getAttribute("last_submit") != null) {
            Instant instant = (Instant) session.getAttribute("last_submit");
            if (Instant.now().minusSeconds(10).compareTo(instant) < 0) {
                return "Don't submitted within 10 seconds";
            } else if (source.length() > 20000) {
                return "Source code too long";
            } else if (source.length() < 2) {
                return "Source code too short";
            }
        }
        session.setAttribute("last_submit", Instant.now());
        return null;
    }

    public static class SubmitCodeObject {
        public SubmitCodeObject() {
        }

        public Boolean isShare() {
            return share.equals("true");
        }

        public void setShare(String share) {
            this.share = share;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getSource() {
            return source;
        }


        public String getLanguage() {
            return language;
        }

        private String source;
        private String language;
        private String share;
    }

    @GetMapping
    public Page<Problem> showProblemList(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<Problem> problemPage;
        if (search != null && search.length() > 0) {
            int spl = search.lastIndexOf("$$");
            if (spl >= 0) {
                String tags = search.substring(spl + 2);
                search = search.substring(0, spl);
                String[] tagNames = tags.split("\\,");
                List<Problem> _problems = problemService.searchActiveProblem(0, 1, search, true).getContent();
                problemPage = problemService.getByTagName(page, PAGE_SIZE, Arrays.asList(tagNames), _problems);
            } else {
                problemPage = problemService.searchActiveProblem(page, PAGE_SIZE, search, false);
            }
        } else {
            problemPage = problemService.getAllActiveProblems(page, PAGE_SIZE);
        }
        for (Problem p : problemPage.getContent()) {
            p.setInput(null);
            p.setOutput(null);
            p.setHint(null);
            p.setSource(null);
            p.setSampleInput(null);
            p.setSampleOutput(null);
        }
        return problemPage;
    }

    @GetMapping("/{id}")
    public Problem showProblem(@PathVariable Long id) {
        Problem problem = problemService.getActiveProblemById(id);
        if (problem == null)
            throw new NotFoundException();
        return problem;
    }

    @PostMapping("/submit/{id}")
    public String submitProblem(@PathVariable("id") Long id,
                                @RequestBody SubmitCodeObject submitCodeObject,
                                HttpServletRequest request) {
        String source = submitCodeObject.getSource();
        boolean share = submitCodeObject.isShare();
        String language = submitCodeObject.getLanguage();
        String _temp = checkSubmitFrequncy(session, source);
        if (_temp != null)
            return _temp;
        @NotNull User user;
        try {
            user = (User) session.getAttribute("currentUser");
            if (userService.getUserById(user.getId()) == null) {// user doesn't login
                log.debug("User not exist");
                return "Please Login";
            }
        } catch (Exception e) {
            return "Please Login";
        }
        Problem problem = problemService.getActiveProblemById(id);
        if (problem == null) {
            return "Problem Not Exist";
        }
        //null检验完成
        Solution solution = solutionService.insertSolution(new Solution(user, problem, language, source, request.getRemoteAddr(), share));
        if (solution == null)
            return "submitted failed";
        try {
//            return restService.submitCode(solution) == null ? "judge failed" : "success";
            judgeService.submitCode(solution);
            return "success";
        } catch (Exception e) {
            return "Internal error";
        }
    }

    @GetMapping("/tags")
    public List<Tag> showTags() {
        return problemService.getAllTags();
    }
}