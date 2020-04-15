package cn.edu.zjnu.acm.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;


@Data
@Entity
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(200)")
    @NotEmpty
    private String url;

    @Column(nullable = false, unique = true, length = 30)
    @NotEmpty
    @Size(min = 1, max = 20)
    private String name;

    @Column(nullable = false, columnDefinition = "VARCHAR(10) default 'c'")
    @NotEmpty
    @Size(min = 1, max = 1)
    private String type;

    public Permission(){}

    public Permission(String name, String url, String type){
        this.name = name;
        this.url = url;
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", pId=").append(id);
        sb.append(", pUrl=").append(url);
        sb.append(", pName=").append(name);
        sb.append("]");
        return sb.toString();
    }
}
