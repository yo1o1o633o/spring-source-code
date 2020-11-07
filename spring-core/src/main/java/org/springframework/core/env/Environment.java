package org.springframework.core.env;

/**
 * 代表当前应用程序运行环境的接口。对应用程序环境的两个关键方面进行建模：配置文件和属性。
 * 与属性访问有关的方法通过PropertyResolver超级接口公开。
 *
 * 一个配置文件的bean定义一个命名的逻辑组，只有当指定的配置文件是活动的容器进行登记
 * 可以将Bean分配给概要文件，无论是用XML定义还是通过注释定义。
 *
 * 属性在几乎所有应用程序中都扮演着重要的角色，
 * 并且可能源自多种来源：属性文件，JVM系统属性，系统环境变量，JNDI，servlet上下文参数，临时属性对象，映射等。
 * 环境对象与属性的关系是为用户提供方便的服务界面，以配置属性源并从中解析属性
 *
 * 在ApplicationContext中管理的Bean可以注册为org.springframework.context.EnvironmentAware EnvironmentAware或@Inject Environment
 * 以便查询配置文件状态或直接解析属性
 *
 * 但是，在大多数情况下，应用程序级Bean无需直接与{@code Environment}进行交互，而可能必须将{@code $ {...}}属性值替换为属性占位符配置程序
 *
 * 环境对象的配置必须通过{@code ConfigurableEnvironment}接口完成，并从所有接口返回
 * {@code AbstractApplicationContext}子类{@code getEnvironment()}方法
 * @author shuai.yang
 */
public interface Environment extends PropertyResolver {
    /**
     * 返回为此环境明确激活的配置文件
     * 概要文件用于创建bean定义的逻辑分组以有条件地进行注册，例如基于部署环境
     * 可以通过将AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME "spring.profiles.active"设置为系统属性
     * 或调用ConfigurableEnvironment#setActiveProfiles(String ...)来激活配置文件。
     * 如果未将任何配置文件明确指定为活动配置文件，则任何#getDefaultProfiles()默认配置文件都会自动激活。
     * @return  配置文件
     */
    String[] getActiveProfiles();

    /**
     * 当没有显式设置活动配置文件时，将默认情况下返回一组配置文件为活动状态
     * @return  配置文件
     */
    String[] getDefaultProfiles();

    /**
     * 返回一个或多个给定的配置文件是否处于活动状态，或者在没有显式活动配置文件的情况下，返回一个或多个给定的配置文件是否包含在默认配置文件集中。
     * 如果个人资料以"!"开头 逻辑相反，即，如果给定的配置文件为not有效，则该方法将返回true。
     * 例如，如果配置文件"p1"处于活动状态或"p2"处于非活动状态，则env.acceptsProfiles("p1","!p2")将返回{@code true}。
     * @throws  IllegalArgumentException 如果使用零参数调用，或者任何配置文件为{@code null}，为空或仅空白
     * @param   profiles    配置文件
     * @return  活动状态
     */
    boolean acceptsProfiles(String... profiles);
}
