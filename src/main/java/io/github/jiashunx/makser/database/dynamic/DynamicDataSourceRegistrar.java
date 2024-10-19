package io.github.jiashunx.makser.database.dynamic;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * @author jiashunx
 */
public class DynamicDataSourceRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        /*
        if (registry.containsBeanDefinition("dynamicDataSourceRegistrar")) {
            return;
        }
        */
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableDynamicDataSource.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationAttributes);
        assert attributes != null;
        int aopOrder = attributes.getNumber("aopOrder");
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("@annotation(io.github.jiashunx.makser.database.dynamic.DSKey)");
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DefaultPointcutAdvisor.class);
        builder.addConstructorArgValue(pointcut);
        builder.addConstructorArgValue(new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                DSKey dsKeyAnnotation = invocation.getThis().getClass().getAnnotation(DSKey.class);
                if (dsKeyAnnotation == null) {
                    dsKeyAnnotation = invocation.getMethod().getAnnotation(DSKey.class);
                }
                if (dsKeyAnnotation != null) {
                    String targetDynamicDataSourceName = dsKeyAnnotation.dynamicDataSourceName();
                    DynamicDataSource defaultDynamicDataSource = DynamicDataSource.getDefaultInstance();
                    if (targetDynamicDataSourceName.isEmpty() || Constants.DEFAULT_DYNAMIC_DATASOURCE_NAME.equals(targetDynamicDataSourceName)) {
                        if (defaultDynamicDataSource == null) {
                            throw new IllegalArgumentException("DSKey未指定动态数据源实例名,使用默认动态数据源实例,未找到默认动态数据源实例");
                        }
                        targetDynamicDataSourceName = (String) defaultDynamicDataSource.getDynamicDataSourceName();
                    }
                    DynamicDataSource targetDynamicDataSource = DynamicDataSource.getInstance(targetDynamicDataSourceName);
                    if (targetDynamicDataSource == null) {
                        throw new IllegalArgumentException(String.format("DSKey指定动态数据源实例名:%s,未找到对应动态数据源实例", targetDynamicDataSourceName));
                    }
                    Object targetDynamicDataSourceKey = dsKeyAnnotation.value();
                    if (((String) targetDynamicDataSourceKey).isEmpty()) {
                        targetDynamicDataSourceKey = targetDynamicDataSource.getDefaultTargetDataSourceKey();
                    }
                    DynamicDataSource dynamicDataSource = DynamicDataSourceKey.getDynamicDataSource();
                    Object dynamicDataSourceKey = DynamicDataSourceKey.getDynamicDataSourceKey();
                    try {
                        DynamicDataSourceKey.set(targetDynamicDataSourceName, targetDynamicDataSourceKey);
                        return invocation.proceed();
                    } finally {
                        DynamicDataSourceKey.set(dynamicDataSource.getDynamicDataSourceName(), dynamicDataSourceKey);
                    }
                }
                return invocation.proceed();
            }
        });
        builder.addPropertyValue("order", aopOrder);
        registry.registerBeanDefinition("dynamicDataSourceAdvisor", builder.getBeanDefinition());
    }
}
