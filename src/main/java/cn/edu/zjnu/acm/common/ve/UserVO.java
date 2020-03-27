package cn.edu.zjnu.acm.common.ve;

import cn.edu.zjnu.acm.entity.User;
import lombok.Data;

import java.time.Instant;

@Data
public class UserVO {
    Instant createtime = Instant.now();
    private Long id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String intro;
    private String salt;
    private int level;

}
