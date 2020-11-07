package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * 用于针对任何基础源解析属性的接口
 *
 * @author shuai.yang
 */
public interface PropertyResolver {

    /**
     * 返回给定的属性键是否可用于解析，即给定的键的值是否为不可用
     * @param   key   属性键
     * @return  是否不可用
     */
    boolean containsProperty(String key);

    /**
     * 返回给定键关联的属性值，如果键不能解析，则返null。
     * @param   key       键入要解析的属性名称
     * @return  属性值
     */
    @Nullable
    String getProperty(String key);

    /**
     * 返回给定键关联的属性值，如果键不能解析，则返回给定的默认值
     * @param   key           键入要解析的属性名称
     * @param   defaultValue  默认值
     * @return  属性值
     */
    String getProperty(String key, String defaultValue);

    /**
     * 返回与给定键关联的属性值，如果键不能解析，则返回null
     * 参数T
     * 第一个 表示是泛型
     * 第二个 表示返回的是T类型的数据
     * 第三个 限制参数类型为T
     * @param   key             键入要解析的属性名称
     * @param   targetType      属性值的预期类型
     * @param   <T>             泛型
     * @return  预期类型的返回值
     */
    @Nullable
    <T> T getProperty(String key, Class<?> targetType);

    /**
     * 返回给定键关联的属性值，如果键不能解析，则返回给定的默认值
     * @param   key           键入要解析的属性名称
     * @param   targetType    属性值的预期类型
     * @param   defaultValue  默认返回值
     * @param   <T>           泛型
     * @return  预期类型的返回值
     */
    <T> T getProperty(String key, Class<?> targetType, T defaultValue);

    /**
     * 返回与给定键关联的属性值, 永不返回null
     * @param   key   键入要解析的属性名称
     * @return  属性值
     * @throws  IllegalStateException    如果给定的文本是null
     */
    String getRequiredProperty(String key) throws IllegalStateException;

    /**
     * 返回与给定键关联的属性值，如果键不能解析，永不返回null
     * @param   key           键入要解析的属性名称
     * @param   targetType    属性值的预期类型
     * @param   <T>           泛型
     * @return  预期类型的返回值
     * @throws  IllegalStateException   如果给定的文本是null
     */
    <T> T getRequiredProperty(String key, Class<?> targetType) throws IllegalStateException;

    /**
     * 在给定的文本中解析$ {...}占位符，将其替换为{@link #getProperty}解析的相应属性值。
     * 带有没有默认值的不可解析的占位符将被忽略并通过原样传递。
     * @param   txt   要解析的字符串
     * @return  解析的字符串
     */
    String resolvePlaceholders(String txt);

    /**
     * 在给定的文本中解析$ {...}占位符，将其替换为{@link #getProperty}解析的相应属性值。 没有默认值的无法解析的占位符将导致引发IllegalArgumentException
     * @param   txt     要解析的字符串
     * @return  解析的字符串
     * @throws  IllegalArgumentException    如果给定的文本为或任何占位符不可解析
     */
    String resolveRequiredPlaceholders(String txt) throws IllegalArgumentException;
}
