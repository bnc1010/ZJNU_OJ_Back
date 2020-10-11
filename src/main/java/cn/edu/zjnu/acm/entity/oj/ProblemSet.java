package cn.edu.zjnu.acm.entity.oj;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.List;
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
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Problem> problems;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Tag> tags;

    @Override
    public String toString(){
        return "id: " + id + " title:" + title + " description:"+ description;
    }
}
