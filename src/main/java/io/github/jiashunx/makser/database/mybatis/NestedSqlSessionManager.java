package io.github.jiashunx.makser.database.mybatis;

import io.github.jiashunx.makser.database.utils.ReflectionUtils;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MyBatis嵌套事务控制
 * @author jiashunx
 */
public class NestedSqlSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(NestedSqlSessionManager.class);

    private static final ThreadLocal<SqlSession> SQL_SESSION_THREAD_LOCAL = new ThreadLocal<>();

    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSessionManager sqlSessionManager;
    private final ThreadLocal<SqlSession> localSqlSession;

    @SuppressWarnings("unchecked")
    public NestedSqlSessionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = Objects.requireNonNull(sqlSessionFactory);
        this.sqlSessionManager = SqlSessionManager.newInstance(this.sqlSessionFactory);
        Field field = Objects.requireNonNull(ReflectionUtils.findField(SqlSessionManager.class, "localSqlSession"));
        field.setAccessible(true);
        this.localSqlSession = (ThreadLocal<SqlSession>) ReflectionUtils.getField(field, this.sqlSessionManager);;
    }

    public static NestedSqlSessionManager newInstance(Reader reader) {
        return new NestedSqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
    }

    public static NestedSqlSessionManager newInstance(Reader reader, String environment) {
        return new NestedSqlSessionManager(new SqlSessionFactoryBuilder().build(reader, environment, null));
    }

    public static NestedSqlSessionManager newInstance(Reader reader, Properties properties) {
        return new NestedSqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, properties));
    }

    public static NestedSqlSessionManager newInstance(InputStream inputStream) {
        return new NestedSqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, null));
    }

    public static NestedSqlSessionManager newInstance(InputStream inputStream, String environment) {
        return new NestedSqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, environment, null));
    }

    public static NestedSqlSessionManager newInstance(InputStream inputStream, Properties properties) {
        return new NestedSqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, properties));
    }

    public static NestedSqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
        return new NestedSqlSessionManager(sqlSessionFactory);
    }

    // 嵌套事务执行
    public void localExecute(Consumer<SqlSession> consumer) {
        localExecute(consumer, false);
    }

    // 嵌套事务执行
    public void localExecute(Consumer<SqlSession> consumer, boolean newTx) {
        localExecute(sqlSession -> {
            consumer.accept(sqlSession);
            return Boolean.TRUE;
        }, newTx);
    }

    // 嵌套事务执行
    public <R> R localExecute(Function<SqlSession, R> function) {
        return localExecute(function, false);
    }

    // 嵌套事务执行
    public <R> R localExecute(Function<SqlSession, R> function, boolean newTx) {
        SqlSession localSqlSessionOfThread = SQL_SESSION_THREAD_LOCAL.get();
        SqlSession localSqlSessionOfManager = this.localSqlSession.get();
        SqlSession currSqlSession = localSqlSessionOfThread;
        if (localSqlSessionOfThread == null && localSqlSessionOfManager != null) {
            currSqlSession = localSqlSessionOfManager;
        }
        if (!newTx && currSqlSession != null) {
            return function.apply(currSqlSession);
        }
        SqlSession sqlSession = null;
        try {
            // sqlSession = this.sqlSessionFactory.openSession(false);
            this.sqlSessionManager.startManagedSession(false);
            sqlSession = this.localSqlSession.get();
            SQL_SESSION_THREAD_LOCAL.set(sqlSession);
            SqlSession sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(SqlSessionFactory.class.getClassLoader(),
                    new Class[] { SqlSession.class }, new SqlSessionInterceptor());
            R r = function.apply(sqlSessionProxy);
            localCommit(sqlSession);
            return r;
        } catch (Throwable throwable) {
            if (sqlSession == null) {
                throw new RuntimeException("数据库操作失败", throwable);
            }
            localRollback(sqlSession);
            throw new RuntimeException("数据库操作失败, 事务回滚成功", throwable);
        } finally {
            try {
                if (this.sqlSessionManager.isManagedSessionStarted()) {
                    this.sqlSessionManager.clearCache();
                    // this.sqlSessionManager.close();
                }
                if (sqlSession != null) {
                    sqlSession.close();
                }
            } finally {
                SQL_SESSION_THREAD_LOCAL.set(localSqlSessionOfThread);
                this.localSqlSession.set(localSqlSessionOfManager);
            }
        }
    }

    public void localCommit(SqlSession sqlSession) {
        if (sqlSession != null) {
            try {
                sqlSession.commit();
                if (logger.isDebugEnabled()) {
                    logger.debug("数据库本地事务提交成功");
                }
            } catch (Throwable throwable) {
                if (logger.isWarnEnabled()) {
                    logger.warn("数据库本地事务提交失败");
                }
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw new RuntimeException("数据库本地事务提交失败", throwable);
            }
        }
    }

    public void localRollback(SqlSession sqlSession) {
        if (sqlSession != null) {
            try {
                sqlSession.rollback();
                if (logger.isDebugEnabled()) {
                    logger.debug("数据库本地事务回滚成功");
                }
            } catch (Throwable throwable) {
                if (logger.isWarnEnabled()) {
                    logger.warn("数据库本地事务回滚失败");
                }
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw new RuntimeException("数据库本地事务回滚失败", throwable);
            }
        }
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return this.sqlSessionFactory;
    }

    public SqlSessionManager getSqlSessionManager() {
        return this.sqlSessionManager;
    }

    public Configuration getConfiguration() {
        return this.sqlSessionManager.getConfiguration();
    }

    public <T> T getMapper(Class<T> type) {
        return this.sqlSessionManager.getMapper(type);
    }

    public boolean hasLocalSqlSession() {
        return SQL_SESSION_THREAD_LOCAL.get() != null;
    }

    private class SqlSessionInterceptor implements InvocationHandler {
        public SqlSessionInterceptor() {
            // Prevent Synthetic Access
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                assert NestedSqlSessionManager.this.localSqlSession != null;
                return method.invoke(NestedSqlSessionManager.this.localSqlSession.get(), args);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }
    }

}
