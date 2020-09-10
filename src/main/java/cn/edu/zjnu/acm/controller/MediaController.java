package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.annotation.IgnoreSecurity;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.constant.StatusCode;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.entity.ImageLog;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.repo.logs.ImageLogRepository;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.Instant;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/media")
public class MediaController {
    private final ImageLogRepository imageLogRepository;
    private final String saveDir = "/onlinejudge/media/";
    private final TokenManager tokenManager;
    private final UserService userService;
    private final long MAX_SIZE = 10 * 1024 * 1024; //最大10M

    public MediaController(ImageLogRepository imageLogRepository, TokenManager tokenManager, UserService userService) {
        this.imageLogRepository = imageLogRepository;
        this.tokenManager = tokenManager;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        initCreating("/onlinejudge/media");
    }

    private void initCreating(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            log.info(filename + " Existed");
        } else {
            if (file.mkdirs())
                log.info(filename + " not existed, created");
            else
                log.error(filename + " not existed, created fail");
        }

    }

    @PostMapping("/upload")
    public RestfulResult uploadImage(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest request) {
        if (multipartFile.isEmpty() || StringUtils.isEmpty(multipartFile.getOriginalFilename())) {
            return new RestfulResult(StatusCode.NOT_FOUND, "no image");
        }
        if (multipartFile.getSize() > MAX_SIZE){
            return new RestfulResult(StatusCode.REQUEST_ERROR, "image too big");
        }
        String filePath;
        String md5;
        String tk = request.getHeader(Constants.DEFAULT_TOKEN_NAME);
        User user = null;
        try {
            TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(tk));
            user = userService.getUserById(tokenModel.getUserId());
            md5 = DigestUtils.md5DigestAsHex(multipartFile.getInputStream());
            filePath = saveDir + md5;
            File saveFile = new File(filePath);
            if (!saveFile.exists()) {
                multipartFile.transferTo(saveFile);//save file
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new RestfulResult(StatusCode.HTTP_FAILURE, "error");
        }
        ImageLog imageLog = new ImageLog(user, request.getRemoteAddr(), filePath, multipartFile.getSize(), "/api/media/" + md5, Instant.now());
        imageLog.saveLog(imageLogRepository);
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success", "/api/media/" + md5);
    }

    @IgnoreSecurity
    @GetMapping("/{filename}")
    public RestfulResult downloadImage(@PathVariable(value = "filename") String filename, HttpServletResponse response) {
        File img = new File(saveDir + filename);
        if (img.exists()) {
            byte[] buffer = new byte[4096];
            try {
                BufferedInputStream stream = new BufferedInputStream(new FileInputStream(img));
                OutputStream os = response.getOutputStream();
                int i = stream.read(buffer);
                while (i != -1) {
                    os.write(buffer, 0, i);
                    i = stream.read(buffer);
                }
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return new RestfulResult(StatusCode.HTTP_FAILURE, "error");
            }
        } else {
            return new RestfulResult(StatusCode.NOT_FOUND, "not found");
        }
        return new RestfulResult(StatusCode.HTTP_SUCCESS, "success");
    }
}
