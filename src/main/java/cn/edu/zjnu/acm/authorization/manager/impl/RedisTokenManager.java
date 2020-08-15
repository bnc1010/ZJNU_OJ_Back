package cn.edu.zjnu.acm.authorization.manager.impl;

import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.constant.Constants;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通过Redis存储和验证token的实现类
 * @see TokenManager
 */
@Component
@Slf4j
public class RedisTokenManager implements TokenManager {

    private RedisTemplate<Long, Object> redis;
    private final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    public void setRedis(@Qualifier("redisTemplate1") RedisTemplate<Long, Object> redis) {
        this.redis = redis;
        //泛型设置成Long后必须更改对应的序列化方案
        redis.setKeySerializer(new JdkSerializationRedisSerializer());
    }

    @Override
    public TokenModel createToken(long userId, String permissionCode, String roleCode, String salt) {
        //uuid
        String uuid = UUID.randomUUID().toString().replace("-", "");
        //时间戳
        String timestamp = SDF.format(new Date());
        //token => userId_timestamp_uuid_permissionCode_roleCode_salt;
        String token = userId + "_" + timestamp + "_" + uuid + "_" + permissionCode + "_" + roleCode + "_" + salt;
        TokenModel model = new TokenModel(userId, uuid, timestamp, permissionCode, roleCode, salt);

        //存储到redis并设置过期时间(有效期为2个小时)
        redis.boundValueOps(userId).set(Objects.requireNonNull(Base64Util.encodeData(token)), Constants.TOKEN_EXPIRES_HOUR, TimeUnit.HOURS);
        return model;
    }

    @Override
    public TokenModel getToken(String authentication) {
        if (authentication == null || authentication.length() == 0) {
            return null;
        }
        String[] param = authentication.split("_");
        if (param.length != 6) {
            return null;
        }

        //使用userId和源token简单拼接成的token，可以增加加密措施
        long userId = Long.parseLong(param[0]);
        String timestamp = param[1];
        String uuid = param[2];
        String permissionCode = param[3];
        String roleCode = param[4];
        String salt = param[5];
        return new TokenModel(userId, uuid, timestamp, permissionCode, roleCode, salt);
    }

    @Override
    public boolean checkToken(TokenModel model) {
        if (model == null) {
            return false;
        }
        Object token = redis.boundValueOps(model.getUserId()).get();
        if (token == null) {
            return false;
        }
        String tokenString = token.toString();
        if (!Objects.equals(Base64Util.decodeData(tokenString), model.getToken())){
            return false;
        }

        //如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间(2个小时)
        redis.boundValueOps(model.getUserId()).expire(Constants.TOKEN_EXPIRES_HOUR, TimeUnit.HOURS);
        return true;
    }

    @Override
    public boolean deleteToken(long userId) {
        if (redis.hasKey(userId)) {
            redis.delete(userId);
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public boolean hasToken(long userId) {
        Object token = redis.boundValueOps(userId).get();
        return token != null;
    }
}
