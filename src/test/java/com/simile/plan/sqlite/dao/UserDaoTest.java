package com.simile.plan.sqlite.dao;

import java.util.Date;

import com.simile.plan.sqlite.model.UserPo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author yitao
 * @Created 2021/08/30
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserDaoTest {

	@Autowired
	private UserDao userDao;

	@Test
	public void create() {
		UserPo userPo = new UserPo(null, "sam", 1, false, new Date(), 1.1, 1.2f);
		userDao.save(userPo);
	}

	@Test
	public void batchCreate() {
		for (int i = 0; i < 10; i++) {
			UserPo userPo = new UserPo(null, "sam" + i, 1, false, new Date(), 1.1, 1.2f);
			userDao.save(userPo);
		}
	}

	@Test
	public void testFindAll() {
		userDao.findAll().stream().forEach(System.out::println);
	}

}
