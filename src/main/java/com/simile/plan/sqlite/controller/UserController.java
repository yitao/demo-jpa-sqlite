package com.simile.plan.sqlite.controller;

import java.util.List;

import com.simile.plan.sqlite.dao.UserDao;
import com.simile.plan.sqlite.model.UserPo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author yitao
 * @Created 2021/08/30
 */
@RestController
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserDao userDao;

	@RequestMapping("/findAll")
	public List<UserPo> findAll() {
		return userDao.findAll();
	}

}
