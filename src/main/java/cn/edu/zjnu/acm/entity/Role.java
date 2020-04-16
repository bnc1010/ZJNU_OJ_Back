package cn.edu.zjnu.acm.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


@Data
@Entity
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Role {
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 角色名
     */
    @Column(nullable = false, unique = true, length = 30)
    @NotEmpty
    @Size(min = 1, max = 20)
    private String name;


    @Column(nullable = false, columnDefinition = "VARCHAR(10) default 'c'")
    @NotEmpty
    @Size(min = 1, max = 1)
    private String type;

    @Column(nullable = false, columnDefinition = "INTEGER default '1000'")
    @NotNull
    private int level;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", roleName=").append(name);
        sb.append("]");
        return sb.toString();
    }
}
