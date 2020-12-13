package cn.edu.zjnu.acm.entity.oj;

import cn.edu.zjnu.acm.entity.User;
import lombok.Data;

import javax.persistence.*;
import java.util.Set;

@Data
@Entity
public class ProblemSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 50, unique = true, nullable = false)
    private String title = "";
    @Column(nullable = false, columnDefinition = "LONGTEXT default ''")
    private String description = "";
    @Column(nullable = false, columnDefinition = "bit(1) default 0")
    private Boolean active = false;
    @Column(nullable = false, columnDefinition = "bit(1) default 1")
    private Boolean isPrivate;
    @ManyToOne(optional = false)
    private User creator;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Problem> problems;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Tag> tags;

    @Override
    public String toString(){
        return "id: " + id + " title:" + title + " description:"+ description;
    }
}
