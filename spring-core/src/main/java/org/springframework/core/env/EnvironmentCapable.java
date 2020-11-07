package org.springframework.core.env;

/**
 * 指示包含并公开{@link Environment}引用的组件的接口
 *
 * 所有Spring应用程序上下文都具有EnvironmentCapable功能，
 * 并且该接口主要用于在接受BeanFactory实例的框架方法中执行{@code instanceof}检查，
 * 以便与环境交互（如果确实可用），该BeanFactory实例实际上可能不是ApplicationContext实例
 *
 * 如上所述,{@link org.springframework.context.ApplicationContext ApplicationContext}继承了EnvironmentCapable
 * 因此公开了一个{@link #getEnvironment()}方法
 * 但是，
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}重新定义了
 * {@link org.springframework.context.ConfigurableApplicationContext＃getEnvironment getEnvironment())}
 * 并缩小了签名范围，以返回{@link ConfigurableEnvironment}
 *
 * 其效果是，一个环境对象是“只读”，直到它从一个ConfigurableApplicationContext访问，此时它也可以被配置。
 * @author shuai.yang
 */
public interface EnvironmentCapable {
    /**
     * 返回与此组件关联的配置文件
     * @return  配置文件
     */
    Environment getEnvironment();
}
