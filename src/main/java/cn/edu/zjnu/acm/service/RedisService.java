package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.oj.Contest;
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

    /**
     *
     * @param userId
     * @return 是否在10秒内有提交题目
     */

    public boolean insertSubmitTime(long userId){
        String key = "submit" + userId;
        try{
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

    /**
     *
     */

    public boolean insertContest(Contest contest){
        String key = "contest" + contest.getId();
        try{
            redis.boundValueOps(key).set(contest, 5, TimeUnit.HOURS);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public Contest getContest(long contestId){
        String key = "contest" + contestId;
        try{
            return (Contest) redis.boundValueOps(key).get();
        }
        catch (Exception e){
            return null;
        }
    }


    public boolean insertUserToContest(Long uId, Long cId){
        String key = "" + uId + "-" + cId;
        try{
            redis.boundValueOps(key).set(uId, 2, TimeUnit.HOURS);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public boolean isUserInContest(Long uId, Long cId){
        String key = "" + uId + "-" + cId;
        if (redis.hasKey(key)){
            return false;
        }
        else{
            return true;
        }
    }
}
