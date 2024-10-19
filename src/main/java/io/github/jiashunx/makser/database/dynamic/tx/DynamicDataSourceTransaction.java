package io.github.jiashunx.makser.database.dynamic.tx;

import io.github.jiashunx.makser.database.dynamic.DynamicDataSource;
import io.github.jiashunx.makser.database.dynamic.DynamicDataSourceKey;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiashunx
 */
public class DynamicDataSourceTransaction implements Transaction {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceTransaction.class);

    private final Map<Object, Connection> connMap;
    protected DataSource dataSource;
    protected TransactionIsolationLevel level;
    protected boolean autoCommit;

    public DynamicDataSourceTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
        this.connMap = new ConcurrentHashMap<>();
        this.dataSource = ds;
        this.level = desiredLevel;
        this.autoCommit = desiredAutoCommit;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DynamicDataSource dynamicDatasource = DynamicDataSourceKey.getDynamicDataSource();
        Object dynamicDataSourceKey = DynamicDataSourceKey.getDynamicDataSourceKey();
        if (!this.connMap.containsKey(dynamicDataSourceKey)) {
            this.connMap.put(dynamicDataSourceKey, openConnection());
        }
        return this.connMap.get(dynamicDataSourceKey);
    }

    @Override
    public void commit() throws SQLException {
        for (Connection connection: this.connMap.values()) {
            if (connection != null && !connection.getAutoCommit()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Committing JDBC Connection [" + connection + "]");
                }
                connection.commit();
            }
        }
    }

    @Override
    public void rollback() throws SQLException {
        for (Connection connection: this.connMap.values()) {
            if (connection != null && !connection.getAutoCommit()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Rolling back JDBC Connection [" + connection + "]");
                }
                connection.rollback();
            }
        }
    }

    @Override
    public void close() throws SQLException {
        for (Connection connection: this.connMap.values()) {
            DataSourceUtils.releaseConnection(connection, this.dataSource);
        }
    }

    protected void setDesiredAutoCommit(Connection connection, boolean desiredAutoCommit) {
        try {
            if (connection.getAutoCommit() != desiredAutoCommit) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
                }
                connection.setAutoCommit(desiredAutoCommit);
            }
        } catch (SQLException e) {
            // Only a very poorly implemented driver would fail here,
            // and there's not much we can do about that.
            throw new TransactionException(
                    "Error configuring AutoCommit.  " + "Your driver may not support getAutoCommit() or setAutoCommit(). "
                            + "Requested setting: " + desiredAutoCommit + ".  Cause: " + e,
                    e);
        }
    }

    protected Connection openConnection() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Opening JDBC Connection");
        }
        Connection connection = dataSource.getConnection();
        if (level != null) {
            connection.setTransactionIsolation(level.getLevel());
        }
        setDesiredAutoCommit(connection, autoCommit);
        return connection;
    }

    @Override
    public Integer getTimeout() throws SQLException {
        return null;
    }
}
