package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.entity.AdminLogs;
import cn.edu.zjnu.acm.entity.CommonLogs;
import cn.edu.zjnu.acm.service.AdminLogService;
import cn.edu.zjnu.acm.service.CommonLogService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/system/")
public class LogController {

    private final CommonLogService commonLogService;
    private final AdminLogService adminLogService;

    public LogController(CommonLogService commonLogService, AdminLogService adminLogService){
        this.commonLogService = commonLogService;
        this.adminLogService = adminLogService;
    }

    @GetMapping("commonlog")
    public RestfulResult getCommonLog(@RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                      @RequestParam(value = "ip", defaultValue = "") String ip,
                                      @RequestParam(value = "url", defaultValue = "") String url,
                                      @RequestParam(value = "startTime", defaultValue = "") String startTime,
                                      @RequestParam(value = "endTime", defaultValue = "") String endTime,
                                      @RequestParam(value = "userId", defaultValue = "-1") Long userId) {

        page =  Math.max(0, page);
        Page<CommonLogs> commonLogsPage = null;
        try{
            commonLogsPage =  commonLogService.getAllWithSearch(ip, url, startTime, endTime,userId, page, pagesize);
        }
        catch (Exception e){
            log.info("/api/system/commonlog error");
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, commonLogsPage);
    }

    @GetMapping("adminlog")
    public RestfulResult getAdminLog(@RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "pagesize", defaultValue = "20") int pagesize,
                                      @RequestParam(value = "ip", defaultValue = "") String ip,
                                      @RequestParam(value = "url", defaultValue = "") String url,
                                      @RequestParam(value = "startTime", defaultValue = "") String startTime,
                                      @RequestParam(value = "endTime", defaultValue = "") String endTime,
                                      @RequestParam(value = "userId", defaultValue = "-1") Long userId) {

        page =  Math.max(0, page);
        Page<AdminLogs> adminLogsPage = null;
        try{
            adminLogsPage =  adminLogService.getAllWithSearch(ip, url, startTime, endTime,userId, page, pagesize);
        }
        catch (Exception e){
            log.info("/api/system/adminlog error");
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, RestfulResult.ERROR);
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, RestfulResult.SUCCESS, adminLogsPage);
    }
}
