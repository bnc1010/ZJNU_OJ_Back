package cn.edu.zjnu.acm.entity.oj;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Entity
@Data
@Slf4j
public class ContestProblem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Problem problem;
    @JsonIgnore
    @ManyToOne
    private Contest contest;
    @Column
    private Long tempId;
    @Column
    private String tempTitle;
    @Column(columnDefinition = "integer default 0")
    private Integer submitted;
    @Column(columnDefinition = "integer default 0")
    private Integer accepted;

    public ContestProblem(Problem problem, Contest contest, Long tempId, String tempTitle) {
        this.problem = problem;
        this.contest = contest;
        this.tempId = tempId;
        this.tempTitle = tempTitle;
    }

    public ContestProblem(Problem problem, String tempTitle, Long tempId) {
        this.problem = problem;
        this.tempTitle = tempTitle;
        this.tempId = tempId;
    }

    public ContestProblem() {
    }

    @Override
    public String toString() {
        return "ContestProblem{" +
                "id=" + id +
                ", problem=" + problem +
                ", tempId=" + tempId +
                ", tempTitle='" + tempTitle + '\'' +
                '}';
    }
}