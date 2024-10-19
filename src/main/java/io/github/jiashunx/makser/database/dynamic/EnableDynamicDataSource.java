package io.github.jiashunx.makser.database.dynamic;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jiashunx
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({DynamicDataSourceRegistrar.class})
public @interface EnableDynamicDataSource {
    // 切面执行顺序
    int aopOrder() default -1000;
}
