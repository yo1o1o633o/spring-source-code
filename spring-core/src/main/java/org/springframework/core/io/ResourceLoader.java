package org.springframework.core.io;


/**
 * @author shuai.yang
 */
public interface ResourceLoader {
    Resource getResource(String location);

    ClassLoader getClassLoader();
}
