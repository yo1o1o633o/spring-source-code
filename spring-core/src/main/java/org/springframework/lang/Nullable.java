package org.springframework.lang;

import java.lang.annotation.*;

/**
 * 一个通用的Spring注释，用于声明被注释的元素在某些情况下可以为{@code null}。
 * 利用JSR 305元注释在支持JSR 305的通用工具上指示Java中的可空性，
 * 并由Kotlin用来推断Spring API的可空性。
 *
 * 应该在参数，返回值和字段级别上使用。 重写方法应重复父{@code @Nullable}批注，除非它们的行为不同。
 *
 * 可以与{@code @NonNullApi}或{@code @NonNullFields}结合使用，以将默认的不可为空的语义覆盖为可为空。
 *
 * @author shuai.yang
 * */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Nullable {
}
