package com.recommend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recommend.entity.Role;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 */
public interface IRoleService extends IService<Role> {

    void setRoleMenu(Integer roleId, List<Integer> menuIds);

    List<Integer> getRoleMenu(Integer roleId);
}
