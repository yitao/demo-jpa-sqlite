package com.simile.plan.sqlite.dao;

import com.simile.plan.sqlite.model.UserPo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Author yitao
 * @Created 2021/08/30
 */
public interface UserDao extends JpaRepository<UserPo, Long> {
}
