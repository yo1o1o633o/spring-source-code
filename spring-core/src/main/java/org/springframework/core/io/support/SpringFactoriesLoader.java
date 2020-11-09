package org.springframework.core.io.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * @author shuai.yang
 */
public abstract class SpringFactoriesLoader {
    public static final String FACTORIES_RESOURCE_LOCATION = "MATE-INF/spring.factories";

    private static final Map<ClassLoader, MultiValueMap<String, String>> cache = new ConcurrentReferenceHashMap<>();

    public static List<String> loadFactoryNames(Class<?> factoryClass, @Nullable ClassLoader classLoader) {
        String factoryClassName = factoryClass.getName();
        return null;
    }

    private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
        MultiValueMap<String, String> result = cache.get(classLoader);
        if (result != null) {
            return result;
        }

        try {
            Enumeration<URL> url = classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) : classLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
        return null;
    }
}
