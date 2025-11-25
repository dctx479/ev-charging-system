package com.ev.charging.service;

import com.ev.charging.common.ResultCode;
import com.ev.charging.dto.RegisterDTO;
import com.ev.charging.entity.User;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务层
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 包含token和用户信息的Map
     */
    public Map<String, Object> login(String username, String password) {
        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 检查账户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("账户已被禁用");
        }

        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        String token = jwtUtil.generateToken(claims);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", buildUserInfo(user));

        return result;
    }

    /**
     * 用户注册（支持个人用户、企业运营商、代充师傅）
     *
     * @param registerDTO 注册信息
     * @return 注册后的用户
     */
    @Transactional
    public User register(RegisterDTO registerDTO) {
        // 验证密码一致性
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new IllegalArgumentException(ResultCode.USER_ALREADY_EXIST.getMessage());
        }

        // 检查手机号是否已存在
        if (userRepository.existsByPhone(registerDTO.getPhone())) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        // 根据用户类型进行额外验证
        validateByUserType(registerDTO);

        // 非普通用户（代充师傅/运营商）需要审核，初始状态为PENDING
        String initialStatus = ("VALET_CHARGER".equals(registerDTO.getUserType()) ||
                "OPERATOR".equals(registerDTO.getUserType())) ? "PENDING" : "ACTIVE";

        // 创建用户实体
        User user = User.builder()
                .username(registerDTO.getUsername())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .nickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername())
                .phone(registerDTO.getPhone())
                .email(registerDTO.getEmail())
                .userType(registerDTO.getUserType())
                .status(initialStatus)
                .balance(java.math.BigDecimal.ZERO)
                .carbonCredits(0)
                .carModel(registerDTO.getCarModel())
                .batteryCapacity(registerDTO.getBatteryCapacity() != null
                        ? java.math.BigDecimal.valueOf(registerDTO.getBatteryCapacity())
                        : null)
                .companyName(registerDTO.getCompanyName())
                .businessLicense(registerDTO.getBusinessLicense())
                .build();

        return userRepository.save(user);
    }

    /**
     * 根据用户类型进行额外验证
     */
    private void validateByUserType(RegisterDTO registerDTO) {
        String userType = registerDTO.getUserType();

        switch (userType) {
            case "OPERATOR":
                // 企业运营商必须提供企业信息
                if (registerDTO.getCompanyName() == null || registerDTO.getCompanyName().trim().isEmpty()) {
                    throw new IllegalArgumentException("企业名称不能为空");
                }
                if (registerDTO.getBusinessLicense() == null || registerDTO.getBusinessLicense().trim().isEmpty()) {
                    throw new IllegalArgumentException("企业统一社会信用代码不能为空");
                }
                break;

            case "VALET_CHARGER":
                // 代充师傅必须提供服务区域和身份证号
                if (registerDTO.getServiceArea() == null || registerDTO.getServiceArea().trim().isEmpty()) {
                    throw new IllegalArgumentException("服务区域不能为空");
                }
                if (registerDTO.getIdCard() == null || registerDTO.getIdCard().trim().isEmpty()) {
                    throw new IllegalArgumentException("身份证号不能为空");
                }
                // 验证身份证号格式
                if (!registerDTO.getIdCard().matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$")) {
                    throw new IllegalArgumentException("身份证号格式不正确");
                }
                break;

            case "USER":
                // 个人用户无额外验证
                break;

            default:
                throw new IllegalArgumentException("不支持的用户类型");
        }
    }

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.USER_NOT_EXIST.getMessage()));
    }

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新后的用户
     */
    @Transactional
    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());

        // 更新允许修改的字段
        if (user.getNickname() != null) {
            existingUser.setNickname(user.getNickname());
        }
        if (user.getPhone() != null) {
            // 检查新手机号是否已被其他账号占用
            if (!user.getPhone().equals(existingUser.getPhone()) &&
                    userRepository.existsByPhone(user.getPhone())) {
                throw new IllegalArgumentException("该手机号已被其他账号绑定");
            }
            existingUser.setPhone(user.getPhone());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        // 更新车辆信息
        if (user.getCarModel() != null) {
            existingUser.setCarModel(user.getCarModel());
        }
        if (user.getBatteryCapacity() != null) {
            existingUser.setBatteryCapacity(user.getBatteryCapacity());
        }

        return userRepository.save(existingUser);
    }

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 参数验证
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("当前密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }

        // 获取用户
        User user = getUserById(userId);

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("当前密码错误");
        }

        // 新旧密码不能相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }

        // 加密新密码并保存
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 构建用户信息（不包含敏感信息）
     *
     * @param user 用户实体
     * @return 用户信息Map
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("userType", user.getUserType());
        userInfo.put("balance", user.getBalance());
        userInfo.put("carbonCredits", user.getCarbonCredits());
        userInfo.put("status", user.getStatus());
        return userInfo;
    }

    // ==================== 管理后台功能 ====================

    /**
     * 获取用户列表（分页、筛选）
     *
     * @param pageable 分页参数
     * @param userType 用户类型筛选
     * @param status   状态筛选
     * @param keyword  搜索关键词
     * @return 用户列表
     */
    public Page<User> getUserList(Pageable pageable, String userType, String status, String keyword) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 用户类型筛选
            if (userType != null && !userType.isEmpty()) {
                predicates.add(cb.equal(root.get("userType"), userType));
            }

            // 状态筛选
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 关键词搜索（用户名、手机号、企业名称）—— 对 % _ \ 转义，防止 LIKE 通配符注入
            if (keyword != null && !keyword.isEmpty()) {
                String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                Predicate usernamePredicate = cb.like(root.get("username"), pattern);
                Predicate phonePredicate = cb.like(root.get("phone"), pattern);
                Predicate companyNamePredicate = cb.like(root.get("companyName"), pattern);
                predicates.add(cb.or(usernamePredicate, phonePredicate, companyNamePredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable);
    }

    /**
     * 审核用户（代充师傅、企业运营商）
     *
     * @param userId   用户ID
     * @param approved 是否通过
     * @param reason   拒绝原因
     */
    public void reviewUser(Long userId, Boolean approved, String reason) {
        User user = getUserById(userId);

        // 检查用户类型
        if (!"VALET_CHARGER".equals(user.getUserType()) && !"OPERATOR".equals(user.getUserType())) {
            throw new IllegalArgumentException("只能审核代充师傅和企业运营商");
        }

        // 检查当前状态
        if (!"PENDING".equals(user.getStatus())) {
            throw new IllegalArgumentException("用户状态不是待审核");
        }

        // 更新状态
        if (approved) {
            user.setStatus("ACTIVE");
            user.setRejectReason(null); // 清除拒绝原因
        } else {
            user.setStatus("REJECTED");
            user.setRejectReason(reason); // 保存拒绝原因
        }

        userRepository.save(user);
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 新状态
     */
    public void updateUserStatus(Long userId, String status) {
        User user = getUserById(userId);

        // 验证状态值
        if (!List.of("ACTIVE", "SUSPENDED", "PENDING", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("无效的状态值");
        }

        user.setStatus(status);
        userRepository.save(user);
    }

    /**
     * 获取用户统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 总用户数
        long totalUsers = userRepository.count();
        statistics.put("totalUsers", totalUsers);

        // 个人用户数
        long personalUsers = userRepository.countByUserType("USER");
        statistics.put("personalUsers", personalUsers);

        // 企业运营商数
        long operators = userRepository.countByUserType("OPERATOR");
        statistics.put("operators", operators);

        // 代充师傅数
        long valetChargers = userRepository.countByUserType("VALET_CHARGER");
        statistics.put("valetChargers", valetChargers);

        // 待审核用户数
        long pendingUsers = userRepository.countByStatus("PENDING");
        statistics.put("pendingUsers", pendingUsers);

        // 活跃用户数
        long activeUsers = userRepository.countByStatus("ACTIVE");
        statistics.put("activeUsers", activeUsers);

        // 已拒绝用户数
        long rejectedUsers = userRepository.countByStatus("REJECTED");
        statistics.put("rejectedUsers", rejectedUsers);

        // 已停用用户数
        long suspendedUsers = userRepository.countByStatus("SUSPENDED");
        statistics.put("suspendedUsers", suspendedUsers);

        return statistics;
    }
}
