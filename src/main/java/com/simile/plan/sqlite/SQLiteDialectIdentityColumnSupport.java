package com.simile.plan.sqlite;


import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * @Author yitao
 * @Created 2021/08/26
 */
public class SQLiteDialectIdentityColumnSupport extends IdentityColumnSupportImpl {

	public SQLiteDialectIdentityColumnSupport(Dialect dialect) {
		super();
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_rowid()";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "integer";
	}
}

