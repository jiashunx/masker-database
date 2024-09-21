package io.github.jiashunx.masker.database.mybatis;

import io.github.jiashunx.makser.database.mybatis.NestedSqlSessionManager;
import io.github.jiashunx.masker.database.sample.entity.TbUser;
import io.github.jiashunx.masker.database.sample.mapper.TbUserMapper;
import org.apache.ibatis.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

/**
 * @author jiashunx
 */
public class NestedSqlSessionManagerTest {

    private NestedSqlSessionManager nestedSqlSessionManager;

    @Before
    public void setUp() {
        // 初始化SQLite3数据库
        // SQLite3JdbcTemplate jdbcTemplate = new SQLite3JdbcTemplate(System.getProperty("user.dir") + "/test.db");
        // 解析SQLite3数据库表结构
        // SQLPackage sqlPackage = SQLite3SQLHelper.loadSQLPackageFromClasspath("ddl-sqlite3.xml");
        // 初始化SQLite3数据库表结构
        // jdbcTemplate.initSQLPackage(sqlPackage);
        // 初始化SqlSessionFactory
        try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
            nestedSqlSessionManager = NestedSqlSessionManager.newInstance(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("创建LocalSqlSessionManager实例失败", e);
        }
    }

    @Test
    public void test() {
        int count = count();
        nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(1));
        Assert.assertEquals(1, count() - count); // 1已提交
        nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(2));
        Assert.assertEquals(2, count() - count); // 1+2已提交

        count = count();
        try {
            nestedSqlSessionManager.getSqlSessionManager().startManagedSession();
            nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(1));
            Assert.assertEquals(0, count() - count); // 1未提交
            nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(2));
            Assert.assertEquals(0, count() - count); // 1+2未提交
            nestedSqlSessionManager.getSqlSessionManager().commit();
            Assert.assertEquals(2, count() - count); // 1+2已提交
        } catch (Throwable throwable) {
            nestedSqlSessionManager.getSqlSessionManager().rollback();
            throw throwable;
        } finally {
            nestedSqlSessionManager.getSqlSessionManager().close();
        }
    }

    @Test
    public void testLocalExecute() {
        int count = count();
        nestedSqlSessionManager.localExecute(sqlSession -> {
            sqlSession.insert(TbUserMapper.class.getName() + ".insert", buildTbUser(1 + 10));
            Assert.assertEquals(0, count() - count); // 1未提交
            nestedSqlSessionManager.localExecute(sqlSession1 -> {
                sqlSession1.insert(TbUserMapper.class.getName() + ".insert", buildTbUser(2 + 10));
                Assert.assertEquals(0, count() - count); // 1+2未提交
            }, true);
            Assert.assertEquals(1, count() - count); // 1未提交,2已提交
            nestedSqlSessionManager.localExecute(sqlSession1 -> {
                nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(3 + 10));
                Assert.assertEquals(1, count() - count); // 1+3未提交,2已提交
                nestedSqlSessionManager.localExecute(sqlSession2 -> {
                    nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(4 + 10));
                    Assert.assertEquals(1, count() - count); // 1+3+4未提交,2已提交
                }, false);
                Assert.assertEquals(1, count() - count); // 1+3+4未提交,2已提交
            }, true);
            Assert.assertEquals(3, count() - count); // 1未提交,2+3+4已提交
        });
        Assert.assertEquals(4, count() - count); // 1+2+3+4已提交
    }

    @Test
    public void testLocalExecute_HasManagedSession() {
        int count = count();
        try {
            nestedSqlSessionManager.getSqlSessionManager().startManagedSession();
            nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(1 + 20));
            Assert.assertEquals(0, count() - count); // 1未提交
            nestedSqlSessionManager.localExecute(sqlSession1 -> {
                sqlSession1.insert(TbUserMapper.class.getName() + ".insert", buildTbUser(2 + 20));
                Assert.assertEquals(0, count() - count); // 1+2未提交
            }, true);
            Assert.assertEquals(1, count() - count); // 1未提交,2已提交
            nestedSqlSessionManager.localExecute(sqlSession1 -> {
                nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(3 + 20));
                Assert.assertEquals(1, count() - count); // 1+3未提交,2已提交
                nestedSqlSessionManager.localExecute(sqlSession2 -> {
                    nestedSqlSessionManager.getMapper(TbUserMapper.class).insert(buildTbUser(4 + 20));
                    Assert.assertEquals(1, count() - count); // 1+3+4未提交,2已提交
                }, true);
                Assert.assertEquals(2, count() - count); // 1+3未提交,2+4已提交
            }, false);
            Assert.assertEquals(2, count() - count); // 1+3未提交,2+4已提交
            nestedSqlSessionManager.getSqlSessionManager().commit();
        } catch (Throwable throwable) {
            nestedSqlSessionManager.getSqlSessionManager().rollback();
            throw throwable;
        } finally {
            nestedSqlSessionManager.getSqlSessionManager().close();
        }
        Assert.assertEquals(4, count() - count); // 1+2+3+4已提交
    }

    private int count() {
        return nestedSqlSessionManager.localExecute(sqlSession -> {
            return sqlSession.selectOne(TbUserMapper.class.getName() + ".countWithCondition", new TbUser());
        }, true);
    }

    private TbUser buildTbUser(int age) {
        TbUser user = new TbUser();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setUserName("Jack");
        user.setUserAge(age);
        user.setCreateTime(new Date());
        user.setLastModifyTime(user.getCreateTime());
        return user;
    }

}
