package org.springframework.boot;

/**
 * Web应用程序可能类型的枚举
 * @author shuai.yang
 */

public enum WebApplicationType {
    /**
     * 该应用程序不应作为Web应用程序运行，也不应启动嵌入式Web服务器
     * */
    NONE,
    /**
     * 该应用程序应作为基于Servlet的Web应用程序运行，并应启动嵌入式Servlet Web服务器
     * */
    SERVLET,
    /**
     * 该应用程序应作为反应式Web应用程序运行，并应启动嵌入式反应式Web服务器
     * */
    REACTIVE
}
