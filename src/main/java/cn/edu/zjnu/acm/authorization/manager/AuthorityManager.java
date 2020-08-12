package cn.edu.zjnu.acm.authorization.manager;


import java.util.ArrayList;

public interface AuthorityManager {

    boolean checkAuthority(String authorityCode, String target);

    String [] getAuthorityCode(long userId);

    ArrayList getRoleByToken(String roleCode);
}
