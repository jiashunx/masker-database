package io.github.jiashunx.makser.database.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jiashunx
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DSKey {

    String value() default "";

    // 动态数据源名称:DynamicDataSource#dynamicDataSourceName,
    String dynamicDataSourceName() default Constants.DEFAULT_DYNAMIC_DATASOURCE_NAME;

}
