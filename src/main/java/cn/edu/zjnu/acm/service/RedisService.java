package cn.edu.zjnu.acm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class RedisService {
    private RedisTemplate<String, Object> redis;

    @Autowired
    public void setRedis(RedisTemplate<String, Object> redis) {
        this.redis = redis;
        //泛型设置成Long后必须更改对应的序列化方案
        redis.setKeySerializer(new JdkSerializationRedisSerializer());
    }

    public boolean insertSubmitTime(long userId){
        try{
            String key = "submit" + userId;
            redis.boundValueOps(key).set(userId, 10, TimeUnit.SECONDS);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public boolean isValidToSubmit(long userId){
        String key = "submit" + userId;
        if (redis.hasKey(key)){
            return false;
        }
        else{
            return true;
        }
    }
}
