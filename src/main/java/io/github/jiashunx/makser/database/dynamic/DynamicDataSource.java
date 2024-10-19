package io.github.jiashunx.makser.database.dynamic;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiashunx
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final Map<Object, DynamicDataSource> DYNAMIC_DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    private final Object dynamicDataSourceName;

    private Object defaultTargetDataSourceKey;

    private boolean defaultDDS = false;

    public DynamicDataSource() {
        this(Constants.DEFAULT_DYNAMIC_DATASOURCE_NAME);
    }

    public DynamicDataSource(Object dynamicDataSourceName) {
        this(dynamicDataSourceName, true);
    }

    public DynamicDataSource(Object dynamicDataSourceName, boolean defaultDDS) {
        this.dynamicDataSourceName = Objects.requireNonNull(dynamicDataSourceName);
        this.defaultDDS = defaultDDS;
        DYNAMIC_DATA_SOURCE_MAP.put(this.dynamicDataSourceName, this);
    }

    @NonNull
    @Override
    protected DataSource determineTargetDataSource() {
        return super.determineTargetDataSource();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceKey.getDynamicDataSourceKey();
    }

    public void setDefaultTargetDataSource(Object defaultTargetDataSourceKey, Object defaultTargetDataSource) {
        this.defaultTargetDataSourceKey = Objects.requireNonNull(defaultTargetDataSourceKey);
        super.setDefaultTargetDataSource(defaultTargetDataSource);
    }

    public Object getDynamicDataSourceName() {
        return dynamicDataSourceName;
    }

    public Object getDefaultTargetDataSourceKey() {
        return defaultTargetDataSourceKey;
    }

    public void setDefaultDDS(boolean defaultDDS) {
        this.defaultDDS = defaultDDS;
    }

    public boolean isDefaultDDS() {
        return defaultDDS;
    }

    public boolean containsDataSource(Object targetDataSourceKey) {
        if (targetDataSourceKey == null) {
            return false;
        }
        if (resolveSpecifiedLookupKey(this.defaultTargetDataSourceKey).equals(targetDataSourceKey)) {
            return true;
        }
        return getResolvedDataSources().containsKey(resolveSpecifiedLookupKey(targetDataSourceKey));
    }

    public static DynamicDataSource getInstance(Object dynamicDataSourceName) {
        if (dynamicDataSourceName == null) {
            return null;
        }
        return DYNAMIC_DATA_SOURCE_MAP.get(dynamicDataSourceName);
    }

    public static DynamicDataSource getDefaultInstance() {
        for (DynamicDataSource dataSource: DYNAMIC_DATA_SOURCE_MAP.values()) {
            if (dataSource.isDefaultDDS()) {
                return dataSource;
            }
        }
        return null;
    }

}
