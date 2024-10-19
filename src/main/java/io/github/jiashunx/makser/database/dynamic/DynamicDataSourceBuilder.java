package io.github.jiashunx.makser.database.dynamic;

import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author jiashunx
 */
public class DynamicDataSourceBuilder {

    private final Map<Object, Object> targetDataSources = new HashMap<>();

    private Object dynamicDataSourceName = Constants.DEFAULT_DYNAMIC_DATASOURCE_NAME;

    private Object defaultTargetDataSourceKey;

    private Object defaultTargetDataSource;

    private boolean defaultDDS = true;

    public DynamicDataSourceBuilder() {}

    public DynamicDataSourceBuilder dynamicDataSourceName(Object dynamicDataSourceName) {
        this.dynamicDataSourceName = Objects.requireNonNull(dynamicDataSourceName);
        return this;
    }

    public DynamicDataSourceBuilder defaultTargetDataSource(Object defaultTargetDataSourceKey, DataSource defaultTargetDataSource) {
        this.defaultTargetDataSourceKey = Objects.requireNonNull(defaultTargetDataSourceKey);
        this.defaultTargetDataSource = defaultTargetDataSource;
        return this;
    }

    public DynamicDataSourceBuilder targetDataSources(Map<Object, Object> targetDataSources) {
        if (targetDataSources != null) {
            this.targetDataSources.putAll(targetDataSources);
        }
        return this;
    }

    public DynamicDataSourceBuilder targetDataSource(Object key, Object targetDataSource) {
        this.targetDataSources.put(key, targetDataSource);
        return this;
    }

    public DynamicDataSourceBuilder defaultDDS(boolean defaultDDS) {
        this.defaultDDS = defaultDDS;
        return this;
    }

    public DynamicDataSource builder() {
        Assert.notNull(this.dynamicDataSourceName, "dynamicDataSourceName must not be null");
        Assert.notNull(this.defaultTargetDataSource, "defaultTargetDataSource must not be null");
        Assert.notEmpty(this.targetDataSources, "targetDataSources must not be empty");
        DynamicDataSource dynamicDataSource = new DynamicDataSource(this.dynamicDataSourceName);
        dynamicDataSource.setDefaultTargetDataSource(this.defaultTargetDataSourceKey, this.defaultTargetDataSource);
        dynamicDataSource.setTargetDataSources(this.targetDataSources);
        dynamicDataSource.setDefaultDDS(this.defaultDDS);
        return dynamicDataSource;
    }

}
