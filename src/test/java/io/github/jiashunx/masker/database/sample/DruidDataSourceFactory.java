package io.github.jiashunx.masker.database.sample;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author jiashunx
 */
public class DruidDataSourceFactory implements DataSourceFactory {

    private Properties properties;

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public DataSource getDataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(this.properties.getProperty("driver"));
        druidDataSource.setUrl(this.properties.getProperty("url"));
        druidDataSource.setUsername(this.properties.getProperty("username"));
        druidDataSource.setPassword(this.properties.getProperty("password"));
        druidDataSource.setDbType(this.properties.getProperty("db_type"));
        druidDataSource.setName("ds-local");
        try {
            druidDataSource.init();
        } catch (SQLException e) {
            throw new RuntimeException("本地数据源初始化异常", e);
        }
        return druidDataSource;
    }
}
