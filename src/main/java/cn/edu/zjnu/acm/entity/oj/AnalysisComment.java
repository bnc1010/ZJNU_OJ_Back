package cn.edu.zjnu.acm.entity.oj;

import cn.edu.zjnu.acm.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisComment extends Comment {

    @JsonIgnore
    @ManyToOne(optional = false)
    private Analysis analysis;

    public AnalysisComment() {
    }

    public AnalysisComment(User user, String text, Comment father, Analysis analysis) {
        super(user, text, father);
        this.analysis = analysis;
    }
}
