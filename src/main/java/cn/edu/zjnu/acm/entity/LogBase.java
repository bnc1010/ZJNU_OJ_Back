package cn.edu.zjnu.acm.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public class LogBase {
    public void saveLog(JpaRepository repository) {
        repository.save(this);
    }
}
