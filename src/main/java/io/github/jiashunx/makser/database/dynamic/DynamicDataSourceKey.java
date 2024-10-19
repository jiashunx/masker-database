package io.github.jiashunx.makser.database.dynamic;

import io.github.jiashunx.makser.database.function.VoidFunc;
import org.springframework.util.Assert;

/**
 * @author jiashunx
 */
public class DynamicDataSourceKey {

    private static final ThreadLocal<DynamicDataSource> THREAD_LOCAL_DATASOURCE = new ThreadLocal<>();

    private static final ThreadLocal<Object> THREAD_LOCAL_KEY = new ThreadLocal<>();

    public DynamicDataSourceKey() {}

    public static void set(Object targetDynamicDataSourceName, Object targetDataSourceKey) {
        Assert.notNull(targetDynamicDataSourceName, "dynamicDataSourceName must not be null");
        Assert.notNull(targetDataSourceKey, "targetDataSourceKey must not be null");
        DynamicDataSource dynamicDataSource = DynamicDataSource.getInstance(targetDynamicDataSourceName);
        Assert.notNull(dynamicDataSource, String.format("根据dynamicDataSourceName[%s]未获取dynamicDataSource实例", targetDynamicDataSourceName));
        Assert.isTrue(dynamicDataSource.containsDataSource(targetDataSourceKey), String.format("dynamicDataSource实例[%s]不存在数据源[%s]", targetDynamicDataSourceName, targetDataSourceKey));
        DynamicDataSourceKey.THREAD_LOCAL_DATASOURCE.set(dynamicDataSource);
        DynamicDataSourceKey.THREAD_LOCAL_KEY.set(targetDataSourceKey);
    }

    public static DynamicDataSource getDynamicDataSource() {
        DynamicDataSource dynamicDataSource = DynamicDataSourceKey.THREAD_LOCAL_DATASOURCE.get();
        if (dynamicDataSource == null) {
            dynamicDataSource = DynamicDataSource.getDefaultInstance();
            Assert.notNull(dynamicDataSource, "default dynamicDataSource must not be null");
            DynamicDataSourceKey.THREAD_LOCAL_DATASOURCE.set(dynamicDataSource);
        }
        return dynamicDataSource;
    }

    public static Object getDynamicDataSourceKey() {
        Object dynamicDataSourceKey = DynamicDataSourceKey.THREAD_LOCAL_KEY.get();
        if (dynamicDataSourceKey == null) {
            dynamicDataSourceKey = getDynamicDataSource().getDefaultTargetDataSourceKey();
            DynamicDataSourceKey.THREAD_LOCAL_KEY.set(dynamicDataSourceKey);
        }
        return dynamicDataSourceKey;
    }

    public static void $switch(Object targetDynamicDataSourceKey, VoidFunc voidFunc) {
        $switch(Constants.DEFAULT_DYNAMIC_DATASOURCE_NAME, targetDynamicDataSourceKey, voidFunc);
    }

    public static void $switch(Object targetDynamicDataSourceName, Object targetDynamicDataSourceKey, VoidFunc voidFunc) {
        DynamicDataSource dynamicDataSource = getDynamicDataSource();
        Object dynamicDataSourceKey = getDynamicDataSourceKey();
        try {
            set(targetDynamicDataSourceName, targetDynamicDataSourceKey);
            voidFunc.apply();
        } finally {
            set(dynamicDataSource.getDynamicDataSourceName(), dynamicDataSourceKey);
        }
    }

}
