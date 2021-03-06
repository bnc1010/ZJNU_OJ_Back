package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.common.exception.AuthorityException;
import cn.edu.zjnu.acm.common.exception.CommonException;
import cn.edu.zjnu.acm.common.utils.StringUtils;
import cn.edu.zjnu.acm.entity.*;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.repo.user.UserRepository;
import cn.edu.zjnu.acm.repo.user.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    RoleService roleService;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TeacherRepository teacherRepository;
    private final UserRoleRepository userRoleRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository, TeacherRepository teacherRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.teacherRepository = teacherRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public Page<User> getUserPage(int page, int size, String search) {
        return userRepository.findAllByUsernameContains(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")), search);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public void grantRoles(long userId, List<Long> roleIds){
        for (long roleId : roleIds){
            UserRole userRole = new UserRole();
            User user = new User();
            user.setId(userId);
            userRole.setUser(user);
            Role role = new Role();
            role.setId(roleId);
            userRole.setRole(role);
            try{
                userRoleRepository.save(userRole);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    @Transactional
    public User registerUser(User u) {
        if (userRepository.findByUsername(u.getUsername()).isPresent())
            throw new AuthorityException("该用户名已存在");
        int level = 1000;
        List<Role> commonRole = roleService.getCommonRole();
        for (Role role : commonRole) {
            level = level > role.getLevel() ? role.getLevel() : level;
        }
        u.setLevel(level);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        u.setPassword(encoder.encode(u.getPassword()));
        UserProfile userProfile = new UserProfile();
        u.setSalt(StringUtils.randomStringFromAlphaAndDigital(4));
        u = userRepository.save(u);
        if (u == null)
            throw new CommonException("注册失败");
        grantRoles(u.getId(), roleService.getCommonRoleId());
        userProfile.setUser(u);
        userProfileRepository.save(userProfile);
        return u;
    }

    public User setUserPassword(User u, String pwd) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        u.setPassword(encoder.encode(pwd));
        return u;
    }

    public void updateUserInfo(User user) {
        int st = userRepository.updateUser(user.getId(), user.getName(), user.getPassword(), user.getEmail(), user.getIntro(), user.getAvatar());
        if (st == 0){
            throw new CommonException("更新失败");
        }
    }

    public boolean checkPassword(String password, String correct) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, correct);
    }

    public User loginUser(User user) {
        User u = userRepository.findByUsername(user.getUsername()).orElse(null);
        if (u == null)
            throw new AuthorityException("该用户名不存在");
        if (checkPassword(user.getPassword(), u.getPassword()))
            return u;
        throw new AuthorityException("用户名或密码错误");
    }

    public List<User> userList() {
        return userRepository.findAll();
    }

    /**
     * get user's permission
     *
     * @param permissionCode
     * @param target
     * @return -1 if normal users, otherwise return teacher privileges.
     */
    public int getUserPermission(String permissionCode, String target) {
        String [] permissionCodes = permissionCode.split("&");
        for (String pc : permissionCodes){
            if (pc.equals("au:")){
                continue;
            }
            if (pc.equals(target)){
                return 1;
            }
        }
        return -1;
    }


    /**
     * update the permission level of one user
     * @param user
     */
    public void updateLevel(User user){
        userRepository.save(user);
    }


    public void robOneRoleOfUser(long userId, long roleId){
        userRoleRepository.robOneRoleOfUser(userId, roleId);
    }
}
