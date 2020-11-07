package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * @author shuai.yang
 */
public abstract class ClassUtils {
    /**
     * 数组类名称的后缀:"[]"
     * */
    public static final String ARRAY_SUFFIX = "[]";
    /**
     * 内部数组类名称的前缀："["
     * */
    public static final String INTERNAL_ARRAY_PREFIX = "[";
    /**
     * 内部非原始数组类名称的前缀:"[L"
     * */
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";
    /**
     * 包分隔符 '.'
     * */
    private static final char PACKAGE_SEPARATOR = '.';
    /**
     * 内部类分隔符 '$'
     * */
    private static final char INNER_CLASS_SEPARATOR = '$';
    /**
     * 使用原始包装器类型作为键并对应的原始类型作为值的映射，例如：Integer.class-> int.class
     * */
    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(8);

    /**
     * 以原始类型名称作为键并使用相应原始类型作为值的映射
     * */
    private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);
    /**
     * 使用通用Java语言类名称作为键，对应的Class作为值进行映射。
     * 主要是为了对远程调用进行有效的反序列化。
     * */
    private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

    private static final Set<Class<?>> javaLanguageInterface;

    static {
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);

        primitiveWrapperTypeMap.forEach((key, value) -> {
            primitiveWrapperTypeMap.put(value, key);
            registerCommonClasses(key);
        });

        Set<Class<?>> primitiveTypes = new HashSet<>(32);
        primitiveTypes.addAll(primitiveWrapperTypeMap.values());
        Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class, double[].class, float[].class, int[].class, long[].class, short[].class);
        primitiveTypes.add(void.class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }

        registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class, Float[].class, Integer[].class, Long[].class, Short[].class);
        registerCommonClasses(Number.class, Number[].class, String.class, String[].class, Class.class, Class[].class, Object.class, Object[].class);
        registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class, Error.class, StackTraceElement.class, StackTraceElement[].class);
        registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class, Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

        Class<?>[] javaLanguageInterfaceArray = {
            Serializable.class, Externalizable.class, Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class
        };
        registerCommonClasses(javaLanguageInterfaceArray);
        javaLanguageInterface = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
    }

    /**
     * 使用ClassUtils缓存注册给定的通用类
     * */
    private static void registerCommonClasses(Class<?>... commonClasses) {
        for (Class<?> clazz : commonClasses) {
            commonClassCache.put(clazz.getName(), clazz);
        }
    }

    /**
     * 返回要使用的默认ClassLoader：通常是线程上下文ClassLoader（如果有）；否则，返回默认值。 加载ClassUtils类的ClassLoader将用作后备。
     * */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            // 返回当前线程的上下文ClassLoader
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // 无法访问线程上下问ClassLoader, 回退
        }
        if (cl == null) {
            // 没有线程上下文类加载器->使用此类的类加载器
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // getClassLoader()返回null, 表示引导类加载器
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // 无法访问系统ClassLoader
                }
            }
        }
        return cl;
    }

    /**
     * 该函数返回基元（例如“ int”）和数组类名称（例如“ String []”）的Class实例。 此外，它还能够解析Java源样式的内部类名称
     * "java.org.springframework.lang.Thread.State" 而不是 "java.org.springframework.lang.Thread$State"
     * */
    public static Class<?> forName(String name, @Nullable ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
        Assert.notNull(name, "name不能为null");

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }

        // "java.org.springframework.lang.String[]" 类型的数组
        if (name.endsWith(ARRAY_SUFFIX)) {
            // 切割掉结尾"[]"
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        // "[Ljava.org.springframework.lang.String;" 类型的数组
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            // 切割掉前后
            String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        // "[[I" or "[[Ljava.org.springframework.lang.String;" 类型的数组, 去掉头部后递归处理其余的前缀
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = getDefaultClassLoader();
        }
        try {
            return (clToUse != null ? clToUse.loadClass(name) : Class.forName(name));
        } catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                // 转换成内部类名.最后一个包分隔符'.'换成'$'
                String innerClassName = name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                try {
                    return (clToUse != null ? clToUse.loadClass(innerClassName) : Class.forName(innerClassName));
                } catch (ClassNotFoundException ex2) {

                }
            }
            throw ex;
        }
    }

    /**
     * 确定是否由所提供的名称来标识存在并且可以被加载。 如果该类或其依赖项之一不存在或无法加载，则将返回false
     * */
    public static boolean ifPresent(String className, @Nullable ClassLoader classLoader) {
        try {
            forName(className, classLoader);
            return true;
        } catch (Throwable ex) {
            // 类或其依赖项之一不存在...
            return false;
        }
    }

    /**
     * 如果适用，将给定的类名称解析为原始类，
     * 根据JVM对原始类的命名规则。
     * 还支持原始数组的JVM内部类名称。
     * 不支持原始数组的后缀“ []”；
     * */
    @Nullable
    public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
        Class<?> result = null;
        // 考虑到将它们放在Map中，大多数类名都将很长，因此值得进行长度检查
        if (name != null && name.length() <= 8) {
            result = primitiveTypeNameMap.get(name);
        }
        return result;
    }
}
