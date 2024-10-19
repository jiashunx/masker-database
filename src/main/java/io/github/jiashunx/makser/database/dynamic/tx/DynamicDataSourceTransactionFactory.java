package io.github.jiashunx.makser.database.dynamic.tx;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;

import javax.sql.DataSource;

/**
 * @author jiashunx
 */
public class DynamicDataSourceTransactionFactory extends SpringManagedTransactionFactory {

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        return new DynamicDataSourceTransaction(dataSource, level, autoCommit);
    }
}
