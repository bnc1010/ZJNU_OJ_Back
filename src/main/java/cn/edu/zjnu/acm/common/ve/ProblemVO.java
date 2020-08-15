package cn.edu.zjnu.acm.common.ve;

import lombok.Data;

@Data
public class ProblemVO {
    private String title;
    private String description;
    private String input;
    private String output;
    private String sampleInput;
    private String sampleOutput;
    private String hint;
    private String source;
    private Integer time;
    private Integer memory;
    private Boolean active;
    private Integer score;
    private String [] tags;

    public ProblemVO() {
    }
}
