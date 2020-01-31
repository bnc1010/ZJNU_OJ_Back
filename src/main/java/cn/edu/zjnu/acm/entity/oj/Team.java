package cn.edu.zjnu.acm.entity.oj;

import cn.edu.zjnu.acm.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Team {
    //不能叫Group  会让sql误会
    public static final String PUBLIC ="public";
    public static final String PRIVATE ="private";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, columnDefinition = "varchar(100) default ''")
    String name;
    @Column(nullable = false, columnDefinition = "LONGTEXT default ''")
    String description;
    @OneToMany(mappedBy = "team")
    List<Teammate> teammates;
    @ManyToOne(optional = false)
    User creator;
    @OneToMany(fetch = FetchType.LAZY)
    List<Contest> contests;
    @Column(nullable = false, columnDefinition = "varchar(50) default 'public'")
    String attend=PUBLIC;
}
