package cn.edu.zjnu.acm.authorization.manager;


public interface AuthorityManager {

    boolean checkAuthority(String authorityCode, String target);

    String [] getAuthorityCode(long userId);
}
