package cn.edu.zjnu.acm.entity;

import lombok.Data;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

@Data
@Entity
@Table(indexes = {@Index(columnList = "operateTime")})
public class CommonLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant operateTime = Instant.now();

    @Column(nullable = false, columnDefinition = "VARCHAR(20) default ''")
    private String ip;

    @Column(nullable = false, columnDefinition = "VARCHAR(200) default ''")
    private String url;

    @Column(columnDefinition = "BIGINT default -1")
    private Long userId;

    @Column(columnDefinition = "VARCHAR(50) default ''")
    private String result;

    public String getNormalTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(operateTime));
    }
}
