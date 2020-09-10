package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.common.exception.NotFoundException;
import cn.edu.zjnu.acm.service.JudgeService;
import cn.edu.zjnu.acm.service.SolutionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@Slf4j
@CrossOrigin
@RestController
public class JudgeController {
    private final JudgeService judgeService;
    private final SolutionService solutionService;

    public JudgeController(JudgeService judgeService, Config config, SolutionService solutionService) {
        this.judgeService = judgeService;
        this.solutionService = solutionService;
    }

    @IgnoreSecurity
    @PostMapping("/judge/callback")
    public String judgeCallback(@RequestBody JudgeController.JudgeCallback callback) {
        System.out.println("*******************************");
        System.out.println(callback.toString());
        try {
            log.info(callback.toString());
            Solution solution = solutionService.getSolutionById(callback.getSubmit_id());
            log.info(solution.toString());
            if (solution == null) {
                return "no this id";
            }
            if (!solution.getResult().equals(Solution.PENDING)) {
                // 只更新PENDING状态的solution
                // 如果rejudge 需要设置solution 为PENDING
                return "success";
            }
            if (callback.getErr() != null) {
                if (callback.getErr().equals("CE")) {
                    solution.setResult(Solution.CE);
                } else {
                    solution.setResult(Solution.SE);
                }
                solution.setInfo(callback.getInfo());
                solutionService.updateSolutionResultInfo(solution);
                return "success";
            } else if (callback.getResults().size() == 0) {
                solution.setResult(Solution.SE);
                solution.setInfo("No results");
                solutionService.updateSolutionResultInfo(solution);
                return "success";
            }
            int cpu = 0;
            int memory = 0;
            int caseNumber = 0;
            for (int i = 0; i < callback.getResults().size(); i++) {
                caseNumber = i + 1;
                JudgeController.JudgeCallback.RunMessage runMessage = callback.getResults().get(i);
                solution.setResult(runMessage.getRunResult());
                if (runMessage.getResult() > 3) {
                    cpu = memory = 0;
                    break;
                }
                cpu = Math.max(cpu, runMessage.getCpu_time());
                memory = Math.max(memory, runMessage.getMemory());
            }
            solution.setCaseNumber(caseNumber);
            solution.setTime(cpu);
            solution.setMemory(memory);
            judgeService.update(solution);
            return "success";
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    @Data
    static class JudgeCallback {
        @NotNull
        private Long submit_id;
        private String err;
        private String info;
        private ArrayList<RunMessage> results;

        @Data
        static class RunMessage {
            public static final String[] code = new String[]{Solution.WA, Solution.AC,
                    Solution.TLE,
                    Solution.TLE,
                    Solution.MLE,
                    Solution.RE,
                    Solution.SE};
            private int cpu_time;
            private int real_time;
            private int memory;
            private int signal;
            private int exit_code;
            private int error;
            private int result;

            public String getRunResult() {
                return code[result + 1];
            }
        }
    }
}
