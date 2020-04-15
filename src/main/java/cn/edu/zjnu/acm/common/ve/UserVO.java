package cn.edu.zjnu.acm.common.ve;

import lombok.Data;

import java.time.Instant;

@Data
public class UserVO {
    Instant createtime;
    private Long id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String intro;
    private String salt;
    private int level;
    private String token;
    private long [] userIds;
    private long [] roleIds;


    @Override
    public String toString() {
        return "{" +
                " createtime=" + createtime +
                " id=" + id +
                " username=" + username +
                " password=" + password +
                " name=" + name +
                " email=" + email +
                " intro=" + intro +
                " email=" + email +
                " salt=" + salt +
                " level=" + level +
                " token=" + token +
                '}';
    }
}
