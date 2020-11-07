package org.springframework.boot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author shuai.yang
 */
public class SpringApplication {
    /**
     * 应用程序非web环境
     * */
    private static final String[] WEB_ENVIRONMENT_CLASSES = {
        "javax.servlet.Servlet", "org.springframework.web.context.ConfigurableWebApplicationContext"
    };

    /**
     * 应用程序web环境
     * */
    private static final String REACTIVE_WEB_ENVIRONMENT_CLASS = "org.springframework." + "web.reactive.DispatcherHandler";

    /**
     * 应用程序响应式web环境
     * */
    private static final String MVC_WEB_ENVIRONMENT_CLASS = "org.springframework." + "web.servlet.DispatcherServlet";

    private Set<Class<?>> primarySources;

    private ResourceLoader resourceLoader;

    private WebApplicationType webApplicationType;

    /**
     * 创建一个新的{@link SpringApplication}实例。应用程序上下文将从指定的主要来源加载bean
     * */
    public SpringApplication(Class<?>... primarySources) {
        this(null, primarySources);
    }

    public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
        this.resourceLoader = resourceLoader;
        Assert.notNull(primarySources, "PrimarySources不能为null");
        // 启动类
        this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
        // 判断当前web应用程序类型
        this.webApplicationType = deduceWebApplicationType();
        getSpringFactoriesInstances(ApplicationContextInitializer.class);
    }

    /**
     * 判断当前Web应用程序类型
     * */
    private WebApplicationType deduceWebApplicationType() {
        if (ClassUtils.ifPresent(REACTIVE_WEB_ENVIRONMENT_CLASS, null) && !ClassUtils.ifPresent(MVC_WEB_ENVIRONMENT_CLASS, null)) {
            return WebApplicationType.REACTIVE;
        }
        for (String className : WEB_ENVIRONMENT_CLASSES) {
            if (!ClassUtils.ifPresent(className, null)) {
                return WebApplicationType.NONE;
            }
        }
        return WebApplicationType.SERVLET;
    }

    public ConfigurableApplicationContext run(String... args) {
        return null;
    }

    /**
     * 静态帮助程序，可用于使用默认设置从指定的源运行
     * */
    public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
        return run(new Class<?>[] {primarySource}, args);
    }

    /**
     * 静态帮助程序，可以使用默认设置和用户提供的参数从指定的源运行
     * */
    public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
        return new SpringApplication(primarySources).run(args);
    }

    private <T> Collection<T> getSpringFactoriesInstances(Class<?> type) {
        return getSpringFactoriesInstances(type, new Class<?>[]{});
    }
    private <T> Collection<T> getSpringFactoriesInstances(Class<?> type, Class<?>[] parameterTypes, Object... args) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        new LinkedHashSet<>();

        return null;
    }
}
