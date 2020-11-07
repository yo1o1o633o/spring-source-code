package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * @author shuai.yang
 */
public abstract class Assert {

    /**
     * 声明一个布尔表达式，如果该表达式的计算结果为{@code false}，则抛出一个{@code IllegalStateException}。
     * */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 声明一个布尔表达式，如果该表达式的求值为{@code false}，则抛出一个{@code IllegalArgumentException}
     * */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言对象不是null
     * @param   object   要检测的对象
     * @param   message  断言失败时使用的异常消息
     * */
    public static void notNull(@Nullable Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
