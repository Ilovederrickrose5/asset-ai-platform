package com.enterprise.asset.business.service;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.SysLog;
import com.enterprise.asset.business.repository.AssetRepository;
import com.enterprise.asset.business.repository.SysLogRepository;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;
    private final SysLogRepository sysLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int CACHE_EXPIRE_MINUTES = 5;
    private static final String CACHE_PREFIX = "asset_list:";

    public AssetService(AssetRepository assetRepository, SysLogRepository sysLogRepository,
                        RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.assetRepository = assetRepository;
        this.sysLogRepository = sysLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // === 当前用户与权限 ===

    private UserDTO getCurrentUserDTO() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof UserDTO userDTO ? userDTO : null;
    }

    private boolean hasRole(UserDTO user, UserRole... roles) {
        if (user == null || user.getRoleCodes() == null) {
            return false;
        }
        for (String code : user.getRoleCodes()) {
            UserRole parsed = UserRole.fromCode(code);
            if (parsed == null) {
                continue;
            }
            for (UserRole target : roles) {
                if (parsed == target) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAdmin(UserDTO user) {
        return hasRole(user, UserRole.ADMIN);
    }

    private boolean isManager(UserDTO user) {
        return hasRole(user, UserRole.MANAGER, UserRole.ASSET_MANAGER, UserRole.DEPT_MANAGER);
    }

    private boolean isLeader(UserDTO user) {
        return hasRole(user, UserRole.LEADER);
    }

    private boolean isDeptScoped(UserDTO user) {
        return isLeader(user) || isManager(user);
    }

    private boolean isVisibleTo(Asset asset, UserDTO user) {
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        if (isDeptScoped(user)) {
            return user.getDeptId() != null && user.getDeptId().equals(asset.getDeptId());
        }
        // 普通员工可查看全公司公共资产，部门仅表示归属
        return true;
    }

    // === 查询 ===

    public List<Asset> getAllAssets() {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }
        if (isAdmin(userDTO)) {
            return assetRepository.findAll();
        }
        if (isDeptScoped(userDTO)) {
            return userDTO.getDeptId() == null ? List.of() : assetRepository.findByDeptId(userDTO.getDeptId());
        }
        // 普通员工可浏览全公司公共资产
        return assetRepository.findAll();
    }

    public Page<Asset> getAssetsWithPagination(int page, int size, String status, String sortBy, String sortDir) {
        return getAssetsWithPagination(page, size, status, sortBy, sortDir, null, false);
    }

    public Page<Asset> getAssetsWithPagination(int page, int size, String status, String sortBy, String sortDir,
                                               String keyword, boolean onlyMine) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return Page.empty();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        String safeSortBy = resolveSortField(sortBy);

        String cacheKey = buildCacheKey(safePage, safeSize, status, safeSortBy, sortDir, keyword, onlyMine, userDTO);

        Page<Asset> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSortBy).descending()
                : Sort.by(safeSortBy).ascending();
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Specification<Asset> specification = buildAssetSpecification(userDTO, status, keyword, onlyMine);
        Page<Asset> result = assetRepository.findAll(specification, pageable);

        writeCache(cacheKey, result);
        return result;
    }

    /** 只允许按实体已有字段排序，避免非法字段导致 500 */
    private String resolveSortField(String sortBy) {
        if (sortBy == null) {
            return "id";
        }
        return switch (sortBy) {
            case "assetNo", "assetName", "purchasePrice", "purchaseDate", "netValue",
                 "originalValue", "status", "useStatus", "deptId", "userId", "createTime",
                 "updateTime", "id" -> sortBy;
            default -> "id";
        };
    }

    private Specification<Asset> buildAssetSpecification(UserDTO userDTO, String status, String keyword,
                                                         boolean onlyMine) {
        Long deptId = !onlyMine && isDeptScoped(userDTO) ? userDTO.getDeptId() : null;
        // 普通员工不按用户过滤，可查看全公司公共资产
        Long userId = onlyMine ? userDTO.getId() : null;
        boolean denyAll = !onlyMine && isDeptScoped(userDTO) && userDTO.getDeptId() == null;
        String normalizedStatus = isEmpty(status) ? null : status;
        String normalizedKeyword = isEmpty(keyword) ? null : keyword.trim().toLowerCase();
        Long scopedDeptId = deptId;
        Long scopedUserId = userId;

        return (root, query, cb) -> {
            if (denyAll) {
                return cb.disjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (scopedDeptId != null) {
                predicates.add(cb.equal(root.get("deptId"), scopedDeptId));
            }
            if (scopedUserId != null) {
                predicates.add(cb.equal(root.get("userId"), scopedUserId));
            }
            if (normalizedStatus != null) {
                predicates.add(cb.equal(root.get("status"), normalizedStatus));
            }
            if (normalizedKeyword != null) {
                String pattern = "%" + normalizedKeyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("assetName")), pattern),
                        cb.like(cb.lower(root.get("assetNo")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Asset getAssetById(Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }
        UserDTO userDTO = getCurrentUserDTO();
        return isVisibleTo(asset, userDTO) ? asset : null;
    }

    public List<Asset> getAssetsByDepartment(Long departmentId) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }
        if (!isAdmin(userDTO) && isDeptScoped(userDTO) && !departmentId.equals(userDTO.getDeptId())) {
            return List.of();
        }
        return assetRepository.findByDeptId(departmentId);
    }

    public long getAssetCountByDepartment(Long departmentId) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return 0;
        }
        if (!isAdmin(userDTO) && isDeptScoped(userDTO) && !departmentId.equals(userDTO.getDeptId())) {
            return 0;
        }
        return assetRepository.countAllAssetsByDepartment(departmentId);
    }

    public List<Asset> getAssetsByUser(Long userId) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }
        if (!isAdmin(userDTO) && !isManager(userDTO) && !userDTO.getId().equals(userId)) {
            return List.of();
        }
        return assetRepository.findByUserId(userId);
    }

    public List<Asset> getAssetsByStatus(String status) {
        UserDTO userDTO = getCurrentUserDTO();
        return filterVisible(assetRepository.findAll().stream()
                .filter(asset -> status != null && status.equals(asset.getStatus()))
                .toList(), userDTO);
    }

    public List<Asset> getAssetsByUseStatus(String useStatus) {
        UserDTO userDTO = getCurrentUserDTO();
        return filterVisible(assetRepository.findAll().stream()
                .filter(asset -> useStatus != null && useStatus.equals(asset.getUseStatus()))
                .toList(), userDTO);
    }

    private List<Asset> filterVisible(List<Asset> assets, UserDTO userDTO) {
        if (userDTO == null) {
            return List.of();
        }
        if (isAdmin(userDTO)) {
            return assets;
        }
        return assets.stream().filter(asset -> isVisibleTo(asset, userDTO)).toList();
    }

    public long getAssetCount() {
        try {
            return assetRepository.countAllAssets();
        } catch (Exception e) {
            logger.error("获取资产总数失败: {}", e.getMessage());
            return 0;
        }
    }

    public long getAssetCountByUser(Long userId) {
        try {
            return assetRepository.countAllAssetsByUser(userId);
        } catch (Exception e) {
            logger.error("获取用户资产数量失败: {}", e.getMessage());
            return 0;
        }
    }

    public List<Asset> getAvailableAssets() {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            return List.of();
        }
        // 资产属于全公司公共资源，部门仅为归属信息，支持跨部门领用
        return assetRepository.findByUserIdIsNull().stream()
                .filter(asset -> !"scrapped".equals(asset.getStatus()))
                .toList();
    }

    public List<String> getAllLocations() {
        return assetRepository.findAll().stream()
                .map(Asset::getLocation)
                .filter(location -> location != null && !location.isEmpty())
                .distinct()
                .toList();
    }

    // === 写操作 ===

    @Transactional
    public Asset createAsset(Asset asset) {
        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            throw new SecurityException("未登录");
        }
        if (!isAdmin(userDTO) && !isManager(userDTO)) {
            throw new SecurityException("无权限创建资产");
        }
        if (!isAdmin(userDTO)) {
            asset.setDeptId(userDTO.getDeptId());
        }

        Asset savedAsset = assetRepository.save(asset);
        clearAssetCache();
        return savedAsset;
    }

    @Transactional
    public Asset updateAsset(Long id, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("资产数据不能为空");
        }
        Asset existingAsset = assetRepository.findById(id).orElse(null);
        if (existingAsset == null) {
            return null;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (!canManage(existingAsset, userDTO)) {
            throw new SecurityException("无权限更新此资产");
        }

        existingAsset.setAssetName(asset.getAssetName());
        existingAsset.setCategoryId(asset.getCategoryId());
        existingAsset.setModel(asset.getModel());
        existingAsset.setUnit(asset.getUnit());
        existingAsset.setPurchasePrice(asset.getPurchasePrice());
        existingAsset.setNetValue(asset.getNetValue());
        existingAsset.setOriginalValue(asset.getOriginalValue());
        existingAsset.setSupplierId(asset.getSupplierId());
        existingAsset.setPurchaseDate(asset.getPurchaseDate());
        existingAsset.setWarrantyPeriod(asset.getWarrantyPeriod());
        existingAsset.setUsefulLife(asset.getUsefulLife());
        existingAsset.setDepreciationMethod(asset.getDepreciationMethod());
        existingAsset.setStatus(asset.getStatus());
        existingAsset.setUseStatus(asset.getUseStatus());
        existingAsset.setCustodianId(asset.getCustodianId());
        existingAsset.setDeptId(isAdmin(userDTO) ? asset.getDeptId() : existingAsset.getDeptId());
        existingAsset.setLocation(asset.getLocation());
        existingAsset.setRemark(asset.getRemark());
        existingAsset.setUserId(asset.getUserId());

        existingAsset.setBorrowStatus(asset.getBorrowStatus());
        existingAsset.setCurrentLocation(asset.getCurrentLocation());
        existingAsset.setBorrowerId(asset.getBorrowerId());
        existingAsset.setBorrowTime(asset.getBorrowTime());
        existingAsset.setExpectedReturnTime(asset.getExpectedReturnTime());

        Asset savedAsset = assetRepository.save(existingAsset);
        clearAssetCache();
        return savedAsset;
    }

    @Transactional
    public boolean deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return false;
        }
        UserDTO userDTO = getCurrentUserDTO();
        if (!canManage(asset, userDTO)) {
            throw new SecurityException("无权限删除此资产");
        }

        assetRepository.deleteById(id);
        clearAssetCache();
        return true;
    }

    /** 资产管理员仅能管理本部门资产，系统管理员可管理全部 */
    private boolean canManage(Asset asset, UserDTO userDTO) {
        if (userDTO == null) {
            return false;
        }
        if (isAdmin(userDTO)) {
            return true;
        }
        return isManager(userDTO)
                && userDTO.getDeptId() != null
                && userDTO.getDeptId().equals(asset.getDeptId());
    }

    @Transactional
    public Asset updateAssetStatus(Long id, String status) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            throw new SecurityException("未登录");
        }

        if (!isAdmin(userDTO) && !isManager(userDTO)) {
            boolean isAssetUser = asset.getUserId() != null && asset.getUserId().equals(userDTO.getId());
            boolean isBorrower = "borrowed".equals(asset.getBorrowStatus())
                    && asset.getBorrowerId() != null
                    && asset.getBorrowerId().equals(userDTO.getId());
            if (!isAssetUser && !isBorrower) {
                throw new SecurityException("无权限更新此资产状态");
            }
        } else if (isManager(userDTO) && !isAdmin(userDTO)) {
            if (userDTO.getDeptId() == null || !userDTO.getDeptId().equals(asset.getDeptId())) {
                throw new SecurityException("部门资产管理员只能更新本部门资产状态");
            }
        }

        asset.setStatus(status);

        if ("in_stock".equals(status) || "idle".equals(status) || "maintenance".equals(status)) {
            asset.setUserId(null);
            asset.setUseStatus("idle");
        }

        Asset savedAsset = assetRepository.save(asset);
        clearAssetCache();

        try {
            SysLog log = new SysLog();
            log.setUserId(userDTO.getId());
            log.setUsername(userDTO.getUsername());
            log.setOperation("资产状态更新：" + asset.getAssetName() + " (" + asset.getAssetNo() + ")");
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        } catch (Exception e) {
            logger.error("添加操作记录失败: {}", e.getMessage());
        }

        return savedAsset;
    }

    @Transactional
    public Asset updateAssetUseStatus(Long id, String useStatus) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return null;
        }

        UserDTO userDTO = getCurrentUserDTO();
        if (userDTO == null) {
            throw new SecurityException("未登录");
        }

        boolean isAssetUser = asset.getUserId() != null && asset.getUserId().equals(userDTO.getId());
        if (!isAssetUser && !isAdmin(userDTO) && !isManager(userDTO)) {
            throw new SecurityException("无权限更新此资产使用状态");
        }
        if (!isAssetUser && isManager(userDTO) && !isAdmin(userDTO)) {
            if (userDTO.getDeptId() == null || !userDTO.getDeptId().equals(asset.getDeptId())) {
                throw new SecurityException("部门资产管理员只能更新本部门资产使用状态");
            }
        }

        asset.setUseStatus(useStatus);

        if ("idle".equals(useStatus)) {
            asset.setUserId(null);
            asset.setStatus("idle");
        }

        Asset savedAsset = assetRepository.save(asset);
        clearAssetCache();
        return savedAsset;
    }

    // === 缓存 ===

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private String buildCacheKey(int page, int size, String status, String sortBy, String sortDir,
                                 String keyword, boolean onlyMine, UserDTO userDTO) {
        // 角色变化会改变可见范围，将权限范围纳入键，避免旧缓存串用
        String scope = isAdmin(userDTO) ? "admin" : isDeptScoped(userDTO) ? "dept" : "all";
        return String.format("%suser_%d:scope_%s:dept_%s:page_%d:size_%d:status_%s:sort_%s_%s:kw_%s:mine_%s",
                CACHE_PREFIX, userDTO.getId(), scope,
                userDTO.getDeptId() != null ? userDTO.getDeptId() : "null",
                page, size, status != null ? status : "all", sortBy, sortDir,
                isEmpty(keyword) ? "all" : keyword.trim(), onlyMine);
    }

    private Page<Asset> readCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached == null) {
                return null;
            }
            String json = cached instanceof String str ? str : objectMapper.writeValueAsString(cached);
            return objectMapper.readValue(json, CachedPage.class).toPage();
        } catch (Exception e) {
            logger.warn("读取资产列表缓存失败，回退数据库查询: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, Page<Asset> page) {
        try {
            String json = objectMapper.writeValueAsString(CachedPage.from(page));
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("写入资产列表缓存失败: {}", e.getMessage());
        }
    }

    private void clearAssetCache() {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(CACHE_PREFIX + "*").count(1000).build())) {
            List<String> keys = cursor.stream().collect(Collectors.toList());
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.warn("清除资产列表缓存失败: {}", e.getMessage());
        }
    }

    /** 可 JSON 序列化的分页快照，避免直接反序列化 Page 接口 */
    public static class CachedPage {
        public List<Asset> content = new ArrayList<>();
        public long totalElements;
        public int totalPages;
        public int number;
        public int size;

        public CachedPage() {
        }

        public static CachedPage from(Page<Asset> page) {
            CachedPage cachedPage = new CachedPage();
            cachedPage.content = new ArrayList<>(page.getContent());
            cachedPage.totalElements = page.getTotalElements();
            cachedPage.totalPages = page.getTotalPages();
            cachedPage.number = page.getNumber();
            cachedPage.size = page.getSize();
            return cachedPage;
        }

        public Page<Asset> toPage() {
            Pageable pageable = PageRequest.of(number, size == 0 ? 10 : size);
            return new PageImpl<>(content, pageable, totalElements);
        }
    }
}
