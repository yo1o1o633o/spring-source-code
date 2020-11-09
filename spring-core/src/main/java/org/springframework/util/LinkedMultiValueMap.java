package org.springframework.util;

import java.io.Serializable;
import java.util.*;

/**
 * 包装了{@link LinkedHashMap}的{@link MultiValueMap}的简单实现，在{@link LinkedList}中存储多个值
 *
 * 此Map实现通常不是线程安全的。它主要设计用于从请求对象公开的数据结构，仅在单个线程中使用。
 * */
public class LinkedMultiValueMap<K, V> implements MultiValueMap<K, V>, Serializable, Cloneable {

    private static final long serialVersionUID = 3801124242820219131L;

    private final Map<K, List<V>> targetMap;

    /**
     * 创建一个包装了{@link LinkedHashMap}的新LinkedMultiValueMap。
     * */
    public LinkedMultiValueMap() {
        this.targetMap = new LinkedHashMap<>();
    }

    /**
     * 创建一个新的LinkedMultiValueMap，其中包装了{@link LinkedHashMap}
     * */
    public LinkedMultiValueMap(int initialCapacity) {
        this.targetMap = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * 复制构造函数：使用与指定Map相同的映射创建一个新的LinkedMultiValueMap。
     * 注意这将是一个浅表副本； 列表条目将被重用，因此不能独立进行修改。
     * */
    public LinkedMultiValueMap(Map<K, List<V>> otherMap) {
        this.targetMap = new LinkedHashMap<>(otherMap);
    }

    /**
     * 获取给定key中列表的第一个元素
     * */
    @Override
    public V getFirst(K key) {
        List<V> values = this.targetMap.get(key);
        return (values != null ? values.get(0) : null);
    }

    /**
     * 向指定key的列表中添加一个值
     * */
    @Override
    public void add(K key, V value) {
        // 获取指定Key的值, 如果为null则创建一个新的LinkedList
        List<V> values = this.targetMap.computeIfAbsent(key, k -> new LinkedList<>());
        // 将值添加到列表中
        values.add(value);
    }

    /**
     * 向指定key的列表中添加一个列表
     * */
    @Override
    public void addAll(K key, List<? extends V> values) {
        List<V> currentValues = this.targetMap.computeIfAbsent(key, k -> new LinkedList<>());
        currentValues.addAll(values);
    }

    /**
     * 将MultiValueMap类型Map数据添加到此Map中
     * */
    @Override
    public void addAll(MultiValueMap<K, V> values) {
        for (Entry<K, List<V>> entry : values.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 向Map中添加一组键值对
     * */
    @Override
    public void set(K key, V value) {
        List<V> values = new LinkedList<>();
        values.add(value);
        targetMap.put(key, values);
    }

    /**
     * 批量向Map中添加键值对
     * */
    @Override
    public void setAll(Map<K, V> values) {
        values.forEach(this::set);
    }

    /**
     * 返回一个新Map, key不变, 值为列表第一个元素
     * */
    @Override
    public Map<K, V> toSingleValueMap() {
        LinkedHashMap<K, V> singleValueMap = new LinkedHashMap<>(this.targetMap.size());
        this.targetMap.forEach((key, value) -> singleValueMap.put(key, value.get(0)));

        return singleValueMap;
    }

    @Override
    public int size() {
        return this.targetMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.targetMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.targetMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.targetMap.containsValue(value);
    }

    @Override
    public List<V> get(Object key) {
        return this.targetMap.get(key);
    }

    @Override
    public List<V> put(K key, List<V> value) {
        return this.targetMap.put(key, value);
    }

    @Override
    public List<V> remove(Object key) {
        return this.targetMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> map) {
        this.targetMap.putAll(map);
    }

    @Override
    public void clear() {
        this.targetMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.targetMap.keySet();
    }

    @Override
    public Collection<List<V>> values() {
        return this.targetMap.values();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return this.targetMap.entrySet();
    }

    /**
     * 创建一个此Map的深拷贝
     * */
    public LinkedMultiValueMap<K, V> deepCopy() {
        LinkedMultiValueMap<K, V> copy = new LinkedMultiValueMap<>(this.targetMap.size());
        this.targetMap.forEach((key, value) -> copy.put(key, new LinkedList<>(value)));

        return copy;
    }

    /**
     * 创建一个此Map的副本
     * */
    @Override
    public LinkedMultiValueMap<K, V> clone() {
        return new LinkedMultiValueMap<>(this);
    }

    @Override
    public boolean equals(Object obj) {
        return this.targetMap.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.targetMap.hashCode();
    }

    @Override
    public String toString() {
        return this.targetMap.toString();
    }
}
