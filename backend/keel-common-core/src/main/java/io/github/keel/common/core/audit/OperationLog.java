package io.github.keel.common.core.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入操作日志的接口方法。
 *
 * <p>由 keel-audit 的切面在方法成功返回后发布事件并异步落库；方法抛异常时事务回滚，不产生日志。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块编码，例如 {@code iam}、{@code system}。
     */
    String module();

    /**
     * 动作编码：CREATE / UPDATE / DELETE / STATUS_CHANGE / ASSIGN。
     */
    String action();
}
