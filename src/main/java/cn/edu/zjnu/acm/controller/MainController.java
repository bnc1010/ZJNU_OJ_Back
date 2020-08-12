package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.service.JudgeService;
import cn.edu.zjnu.acm.service.SolutionService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@CrossOrigin
@RestController
public class MainController {
    private final Config config;

    public MainController(JudgeService judgeService, Config config, SolutionService solutionService) {
        this.config = config;
    }

    @GetMapping("/")
    public ModelAndView home() {
        return new ModelAndView("index");
    }

    @GetMapping("/donate")
    public ModelAndView donatePage() {
        return new ModelAndView("donate");
    }

    @IgnoreSecurity
    @GetMapping("/api/notice")
    public RestfulResult getNotice() {
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", config.getNotice());
    }
}