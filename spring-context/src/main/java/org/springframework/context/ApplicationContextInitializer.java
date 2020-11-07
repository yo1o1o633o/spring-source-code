package org.springframework.context;

/**
 * 回调接口，用于在刷新ConfigurableApplicationContext＃refresh（）之前初始化Spring ConfigurableApplicationContext。
 * 通常在需要对应用程序上下文进行一些编程初始化的Web应用程序中使用。
 * 例如，针对ConfigurableApplicationContext＃getEnvironment（）注册属性源或激活配置文件。
 * 请参见{@code ContextLoader}和{@code FrameworkServlet}支持，分别用于声明“ contextInitializerClasses”上下文参数和初始化参数。
 *
 * @author shuai.yang
 */
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

    /**
     * 初始化给定的应用程序上下文
     * @param  applicationContext  要配置的应用程序
     * */
    void initializer(C applicationContext);
}
