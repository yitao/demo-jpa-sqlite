package com.simile.plan.sqlite;


import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.AbstractAnsiTrimEmulationFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.type.StandardBasicTypes;

/**
 * sqlite 方言
 *
 * @Author yitao
 * @Created 2021/08/26
 */
public class SQLiteDialect extends Dialect {
	private static final String FIELD_TYPE_NULL = "NULL";
	private static final String FIELD_TYPE_INTEGER = "INTEGER";
	private static final String FIELD_TYPE_REAL = "REAL";
	private static final String FIELD_TYPE_TEXT = "TEXT";
	private static final String FIELD_TYPE_BLOB = "BLOB";

	private final UniqueDelegate uniqueDelegate;
	public SQLiteDialect() {
		registerColumnTypes();
		registerFunctions();
		uniqueDelegate = new SQLiteUniqueDelegate(this);
	}

	/**
	 * 注册sqlite中的字段类型
	 * 将Types中的字段类型映射为数据库中具体的字段类型
	 * sqlite中数据类型一共有以下5中，null,integer,real,text,blob
	 * 参考sqlite官方说明：https://www.sqlite.org/datatype3.html
	 */
	protected void registerColumnTypes() {
		registerColumnType(Types.NULL, FIELD_TYPE_NULL);
		for (int type : getIntegerTypes()) {
			registerColumnType(type, FIELD_TYPE_INTEGER);
		}
		for (int type : getRealTypes()) {
			registerColumnType(type, FIELD_TYPE_REAL);
		}
		for (int type : getTextTypes()) {
			registerColumnType(type, FIELD_TYPE_TEXT);
		}
		for (int type : getBlobTypes()) {
			registerColumnType(type, FIELD_TYPE_BLOB);
		}
	}


	protected int[] getIntegerTypes() {
		return new int[]{Types.BIT, Types.CHAR, Types.INTEGER, Types.BIGINT, Types.TIMESTAMP};
	}

	protected int[] getRealTypes() {
		return new int[]{Types.NUMERIC, Types.FLOAT, Types.DOUBLE, Types.DECIMAL};
	}

	protected int[] getTextTypes() {
		return new int[]{Types.VARCHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.LONGNVARCHAR};
	}

	protected int[] getBlobTypes() {
		return new int[]{Types.BLOB, Types.CLOB, Types.NCLOB, Types.BINARY, Types.LONGVARBINARY};
	}


	/**
	 * 注册sqlite中的函数
	 */
	protected void registerFunctions() {
		registerFunction("concat", new VarArgsSQLFunction(StandardBasicTypes.STRING, "", "||", ""));
		registerFunction("mod", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1 % ?2"));
		registerFunction("quote", new StandardSQLFunction("quote", StandardBasicTypes.STRING));
		registerFunction("random", new NoArgSQLFunction("random", StandardBasicTypes.INTEGER));
		registerFunction("round", new StandardSQLFunction("round"));
		registerFunction("substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING));
		registerFunction("trim", new AbstractAnsiTrimEmulationFunction() {
			@Override
			protected SQLFunction resolveBothSpaceTrimFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "trim(?1)");
			}

			@Override
			protected SQLFunction resolveBothSpaceTrimFromFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "trim(?2)");
			}

			@Override
			protected SQLFunction resolveLeadingSpaceTrimFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "ltrim(?1)");
			}

			@Override
			protected SQLFunction resolveTrailingSpaceTrimFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "rtrim(?1)");
			}

			@Override
			protected SQLFunction resolveBothTrimFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "trim(?1, ?2)");
			}

			@Override
			protected SQLFunction resolveLeadingTrimFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "ltrim(?1, ?2)");
			}

			@Override
			protected SQLFunction resolveTrailingTrimFunction() {
				return new SQLFunctionTemplate(StandardBasicTypes.STRING, "rtrim(?1, ?2)");
			}
		});
	}


	private static final SQLiteDialectIdentityColumnSupport IDENTITY_COLUMN_SUPPORT =
			new SQLiteDialectIdentityColumnSupport(new SQLiteDialect());

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return IDENTITY_COLUMN_SUPPORT;
	}


	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow(selection);
			return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}
	};

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}



	@Override
	public boolean supportsLockTimeouts() {
		// may be http://sqlite.org/c3ref/db_mutex.html ?
		return false;
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	//current timestamp support process begin
	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp";
	}

	//current timestamp support process end

	private static final int SQLITE_BUSY = 5;
	private static final int SQLITE_LOCKED = 6;
	private static final int SQLITE_IOERR = 10;
	private static final int SQLITE_CORRUPT = 11;
	private static final int SQLITE_NOTFOUND = 12;
	private static final int SQLITE_FULL = 13;
	private static final int SQLITE_CANTOPEN = 14;
	private static final int SQLITE_PROTOCOL = 15;
	private static final int SQLITE_TOOBIG = 18;
	private static final int SQLITE_CONSTRAINT = 19;
	private static final int SQLITE_MISMATCH = 20;
	private static final int SQLITE_NOTADB = 26;

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final int errorCode = JdbcExceptionHelper.extractErrorCode(sqlException) & 0xFF;
				if (errorCode == SQLITE_TOOBIG || errorCode == SQLITE_MISMATCH) {
					return new DataException(message, sqlException, sql);
				} else if (errorCode == SQLITE_BUSY || errorCode == SQLITE_LOCKED) {
					return new LockAcquisitionException(message, sqlException, sql);
				} else if ((errorCode >= SQLITE_IOERR && errorCode <= SQLITE_PROTOCOL) || errorCode == SQLITE_NOTADB) {
					return new JDBCConnectionException(message, sqlException, sql);
				}
				return null;
			}
		};
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int errorCode = JdbcExceptionHelper.extractErrorCode(sqle) & 0xFF;
			if (errorCode == SQLITE_CONSTRAINT) {
				return extractUsingTemplate("constraint ", " failed", sqle.getMessage());
			}
			return null;
		}
	};


	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public boolean hasAlterTable() {
		return false;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getDropForeignKeyString() {
		throw new UnsupportedOperationException("No drop foreign key syntax supported by SQLiteDialect");
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName, String[] foreignKey, String referencedTable,
			String[] primaryKey, boolean referencesPrimaryKey) {
		throw new UnsupportedOperationException("No add foreign key syntax supported by SQLiteDialect");
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		throw new UnsupportedOperationException("No add primary key syntax supported by SQLiteDialect");
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}


	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	/**
	 * 表达式数量限制
	 * http://sqlite.org/limits.html#max_variable_number
	 * @return
	 */
	@Override
	public int getInExpressionCountLimit() {
		return 1000;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public String getSelectGUIDString() {
		return "select hex(randomblob(16))";
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	static class SQLiteUniqueDelegate extends DefaultUniqueDelegate {
		public SQLiteUniqueDelegate(Dialect dialect) {
			super(dialect);
		}

		@Override
		public String getColumnDefinitionUniquenessFragment(Column column) {
			return " unique";
		}
	}
}

