package io.github.jiashunx.makser.database.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Objects;

/**
 * Spring本地事务控制
 *
 * @author jiashunx
 */
public class LocalTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalTransactionManager.class);

    private static final ThreadLocal<TransactionStatus> TRANSACTION_STATUS_THREAD_LOCAL = new ThreadLocal<>();

    private final PlatformTransactionManager transactionManager;

    public LocalTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }

    public void newLocalTransaction() {
        this.newLocalTransaction(-1);
    }

    public void newLocalTransaction(int timeout) {
        TransactionStatus txStatus = TRANSACTION_STATUS_THREAD_LOCAL.get();
        if (txStatus != null && !txStatus.isCompleted()) {
            throw new RuntimeException("当前线程存在已本地事务");
        }
        this.clearThreadLocal();
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setTimeout(timeout);
        def.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        TRANSACTION_STATUS_THREAD_LOCAL.set(this.transactionManager.getTransaction(def));
        if (logger.isDebugEnabled()) {
            logger.debug("开启数据库本地事务,超时时间(单位秒):{}", timeout);
        }
    }

    public void localCommit() {
        TransactionStatus txStatus = TRANSACTION_STATUS_THREAD_LOCAL.get();
        if (txStatus != null) {
            if (txStatus.isCompleted()) {
                this.clearThreadLocal();
                throw new RuntimeException("数据库本地事务已完成(提交/回滚),禁止再次提交");
            }
            try {
                this.transactionManager.commit(txStatus);
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
            } finally {
                this.clearThreadLocal();
            }
        }
    }

    public void localRollback() {
        TransactionStatus txStatus = TRANSACTION_STATUS_THREAD_LOCAL.get();
        if (txStatus != null) {
            if (txStatus.isCompleted()) {
                this.clearThreadLocal();
                throw new RuntimeException("数据库本地事务已完成(提交/回滚),禁止再次回滚");
            }
            try {
                this.transactionManager.rollback(txStatus);
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
            } finally {
                this.clearThreadLocal();
            }
        }
    }

    public void clearThreadLocal() {
        TRANSACTION_STATUS_THREAD_LOCAL.remove();
    }

    public boolean hasLocalTransaction() {
        return TRANSACTION_STATUS_THREAD_LOCAL.get() != null;
    }

}
