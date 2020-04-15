package cn.edu.zjnu.acm.service;


import cn.edu.zjnu.acm.authorization.manager.TokenManager;
import cn.edu.zjnu.acm.authorization.model.TokenModel;
import cn.edu.zjnu.acm.common.exception.AuthorityException;
import cn.edu.zjnu.acm.common.utils.Base64Util;
import cn.edu.zjnu.acm.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userOperationService")
public class UserOperationService{
    @Autowired
    UserService userService;
    @Autowired
    TokenManager tokenManager;

    public User checkOperationToUserByToken(String operatorToken, long target) {
        if (operatorToken == null){
            throw new AuthorityException("token为空");
        }

        TokenModel tokenModel = tokenManager.getToken(Base64Util.decodeData(operatorToken));

        if (tokenModel == null){
            throw new AuthorityException("token无效");
        }

        if (target == -1){
            return null;
        }

        User operator = userService.getUserById(tokenModel.getUserId());

        if (operator == null){
            throw new AuthorityException("token无效");
        }

        User targetUser = userService.getUserById(target);
        if (targetUser == null){
            throw new AuthorityException("目标用户无效，id：" + target);
        }

        if (targetUser.getLevel() <= operator.getLevel()){
            throw new AuthorityException("权限不足，无法完成该操作");
        }

        return operator;
    }

    public boolean checkTokenNotEmpty(String token) {
        if (token == null){
            throw new AuthorityException("token为空");
        }
        return true;
    }
}
