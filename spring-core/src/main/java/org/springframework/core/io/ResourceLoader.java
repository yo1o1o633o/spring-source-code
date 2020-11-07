package org.springframework.core.io;

import com.sun.istack.internal.Nullable;

/**
 * @author shuai.yang
 */
public interface ResourceLoader {
    Resource getResource(String location);

    @Nullable
    ClassLoader getClassLoader();
}
