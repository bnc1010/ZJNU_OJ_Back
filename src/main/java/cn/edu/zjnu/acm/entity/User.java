package cn.edu.zjnu.acm.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.time.Instant;

@Data
@Entity
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements Cloneable, Comparable {
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    Instant createtime = Instant.now();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false, length = 30)
    @NotEmpty
    @Size(min = 6, max = 35)
    private String username;
    @Column(nullable = false, length = 60)
    @NotEmpty
    @Size(min = 6, max = 60)
    private String password;
    @Column(nullable = false, columnDefinition = "VARCHAR(250) default ''")
    @NotEmpty
    @Size(min = 1, max = 30)
    private String name;
    @Column(nullable = false, columnDefinition = "VARCHAR(250) default ''")
    @Size(min = 4, max = 200)
    @Email
    private String email;
    @Column(nullable = false, columnDefinition = "VARCHAR(250) default ''")
    @Size(max = 250)
    private String intro;
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private String salt;
    @Column(nullable = false, columnDefinition = "INTEGER default 1000")
    private int level;
    @Column(columnDefinition = "VARCHAR(100) default ''")
    private String avatar;


    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private UserProfile userProfile;



    public User() {
        this.username = "";
        this.password = "";
        this.email = "";
        this.name = "";
        this.intro = "";
        this.salt = "";
        this.level = 1000;
        this.avatar = "";
    }

    public User(@NotEmpty @Size(min = 6, max = 30) String username, @NotEmpty @Size(min = 6, max = 30) String password) {
        this.username = username;
        this.password = password;
        this.email = "";
        this.name = "";
        this.intro = "";
        this.salt = "";
        this.level = 1000;
        this.avatar = "";
    }

    public User(@NotEmpty @Size(min = 6, max = 30) String username, @NotEmpty @Size(min = 6, max = 30) String password, @NotEmpty @Size(min = 1, max = 30) String name, @Size(min = 4, max = 200) String email, @Size(max = 250) String intro, String salt, int level, @Size(max = 100) String avatar) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.intro = intro;
        this.salt = salt;
        this.level = level;
        this.avatar = avatar;
    }

    public User clone() throws CloneNotSupportedException {
        return (User) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return getId().equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public int compareTo(Object o) {
        return getId().compareTo(((User) o).getId());
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", intro='" + intro + '\'' +
                ", salt='" + salt + '\'' +
                ", level='" + level + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }

    public User hideInfo() {
        setPassword(null);
        setEmail(null);
        setIntro(null);
        setCreatetime(null);
        setSalt(null);
        setLevel(-1);
        return this;
    }
}
