package com.simile.plan.sqlite.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author yitao
 * @Created 2021/08/30
 */
@Entity(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPo {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name")
	private String name;

	private int age;

	private boolean human;

	private Date birthday;

	private double score;

	private float score2;

}
