package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

public interface MultiValueMap<K, V> extends Map<K, List<V>> {
    /**
     * 返回给定键的第一个值
     * */
    @Nullable
    V getFirst(K key);

    /**
     * 将给定的单个值添加到给定键的当前值列表中
     * */
    void add(K key, @Nullable V value);

    /**
     * 将给定列表的所有值添加到给定键的当前值列表中
     * */
    void addAll(K key, List<? extends V> values);

    /**
     * 将给定{@code MultiValueMap}的所有值添加到当前值
     * */
    void addAll(MultiValueMap<K, V> values);

    /**
     * 在给定的键下设置给定的单个值
     * */
    void set(K key, @Nullable V value);

    /**
     * 批量设置给定值
     * */
    void setAll(Map<K, V> values);

    /**
     * 返回此{@code MultiValueMap}中包含的第一个值
     * */
    Map<K, V> toSingleValueMap();
}
