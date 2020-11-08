package org.springframework.util;

import org.springframework.lang.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一个{@link ConcurrentHashMap}，它对{@code key}和{@code value}使用{@link ReferenceType#SOFT}软引用或{@linkplain ReferenceType#WEAK}弱引用。
 *
 * 此类可以用作{@code Collections.synchronizedMap(new WeakHashMap<K,Reference<V>>())}的替代方法，以便在并发访问时提供更好的性能。
 * 此实现遵循与{@link ConcurrentHashMap}相同的设计约束，但支持{@code null}值和{@code null}键。
 *
 * 使用引用意味着不能保证放置在Map中的项目随后将可用。 垃圾收集器可能会随时丢弃引用，因此似乎未知线程正在静默删除条目。
 *
 * @author shuai.yang
 */
public class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    /**
     * 默认的初始容量
     * */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * 默认的容量调整阈值
     * */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 默认的并发线程数
     * */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 默认的引用类型, 软引用
     * */
    private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;

    /**
     * 并发线程的最大值
     * */
    private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

    /**
     * 段数组的最大值
     * */
    private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;

    /**
     * 段数组, 此Map支持并发操作, 有多少个线程操作就有多少个段, 每个线程操作此数组中的一个元素
     * */
    private final Segment[] segments;

    private final float loadFactor;

    /**
     * 参考类型：SOFT或WEAK. 用于区分软引用还是弱引用.
     * */
    private final ReferenceType referenceType;

    /**
     * 用于计算segments数组的大小和哈希值索引的shift值
     * */
    private final int shift;

    /**
     * 创建一个新的实例, 传入默认值
     * */
    public ConcurrentReferenceHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * 创建一个新的实例, 自定义初始容量, 其他传入默认值
     * @param   initialCapacity   Map初始容量
     * */
    public ConcurrentReferenceHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * 创建一个新的实例, 自定义初始容量和负载系数, 其他传入默认值
     * @param   initialCapacity   Map初始容量
     * @param   loadFactor        负载系数。当每个表的平均引用数超过此值时，将尝试调整大小
     * */
    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * 创建一个新的实例, 自定义初始容量和预期线程数, 其他传入默认值
     * @param   initialCapacity   Map初始容量
     * @param   concurrencyLevel  并发写入映射的预期线程数
     * */
    public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * 创建一个新的实例, 自定义初始容量和用于输入的引用类型, 其他传入默认值
     * @param   initialCapacity   Map初始容量
     * @param   referenceType     用于输入的引用类型
     * */
    public ConcurrentReferenceHashMap(int initialCapacity, ReferenceType referenceType) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, referenceType);
    }

    /**
     * 创建一个新的实例, 传入默认用于输入的引用类型, 其他自定义
     * @param   initialCapacity      Map初始容量
     * @param   loadFactor           负载系数。当每个表的平均引用数超过此值时，将尝试调整大小
     * @param   concurrencyLevel     并发写入映射的预期线程数
     * */
    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
    }

    /**
     * 创建一个新的实例
     * 例如 Map初始16个线程, 那么segments元素16个, 每个元素内的初始容量是2
     * */
    @SuppressWarnings("unchecked")
    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, ReferenceType referenceType) {
        Assert.isTrue(initialCapacity >= 0, "初始容量不能为负");
        Assert.isTrue(loadFactor > 0f, "负载系数必须为正");
        Assert.isTrue(concurrencyLevel > 0, "并发级别必须为正");
        Assert.notNull(referenceType, "referenceType不能为null");

        this.loadFactor = loadFactor;
        // 此处是根据传入线程数和最大线程数之中最小值计算出偏移量
        // 如果传入的线程数小于最大线程, 那么按照传入值来计算偏移量, 如果大于最大线程, 那么按最大线程值来计算偏移量.
        this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
        // 用1向左位移偏移量个位数, 获取到线程数.
        int size = 1 << this.shift;
        this.referenceType = referenceType;
        // 猜想: 计算出每个线程分配的容量? 减一目的是四舍五入吗?
        int roundedUpSegmentCapacity = (int) ((initialCapacity + size - 1L) / size);
        // 根据线程数创建段数组, 有几个线程就创建几个段
        this.segments = (Segment[]) Array.newInstance(Segment.class, size);
        for (int i = 0; i < this.segments.length; i++) {
            // 每个段的初始容量
            this.segments[i] = new Segment(roundedUpSegmentCapacity);
        }
    }

    /**
     * 返回扩容阈值
     * */
    protected final float getLoadFactor() {
        return this.loadFactor;
    }

    /**
     * 返回有多少个段
     * */
    protected final int getSegmentsSize() {
        return this.segments.length;
    }

    /**
     * 返回指定段
     * */
    protected final Segment getSegment(int index) {
        return this.segments[index];
    }

    /**
     * 返回{@link ReferenceManager}的工厂方法。
     * 每个{@link Segment}都会调用一次此方法。
     * */
    protected ReferenceManager createReferenceManager() {
        return new ReferenceManager();
    }

    /**
     * 获取给定对象的哈希，应用附加的哈希函数以减少冲突。
     * 此实现使用与{@link ConcurrentHashMap}相同的Wang/Jenkins算法。
     * 子类可以重写以提供替代的哈希。
     * */
    protected int getHash(@Nullable Object o) {
        int hash = o == null ? 0 : o.hashCode();
        hash += (hash << 15) ^ 0xffffcd7d;
        hash ^= (hash >>> 10);
        hash += (hash << 3);
        hash ^= (hash >>> 6);
        hash += (hash << 2) + (hash << 14);
        hash ^= (hash >>> 16);
        return hash;
    }

    /**
     * 根据KEY获取VALUE, 没找到返回null
     * */
    @Override
    @Nullable
    public V get(@Nullable Object key) {
        Entry<K, V> entry = getEntryIfAvailable(key);
        return (entry != null ? entry.getValue() : null);
    }

    /**
     * 根据KEY获取VALUE, 没找到返回传入的默认值
     * */
    @Override
    @Nullable
    public V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
        Entry<K, V> entry = getEntryIfAvailable(key);
        return (entry != null ? entry.getValue() : defaultValue);
    }

    /**
     * 判断Map中是否有指定KEY.
     * */
    @Override
    public boolean containsKey(@Nullable Object key) {
        Entry<K, V> entry = getEntryIfAvailable(key);
        return (entry != null && ObjectUtils.nullSafeEquals(entry.getKey(), key));
    }

    /**
     * 根据Map的key获取指定条目, 如果有就返回, 否则返回null
     * */
    @Nullable
    private Entry<K, V> getEntryIfAvailable(@Nullable Object key) {
        // 拉取key的节点引用对象
        Reference<K, V> reference = getReference(key, Restructure.WHEN_NECESSARY);
        return (reference != null ? reference.get() : null);
    }

    /**
     * 通过Map的key找到对应的节点
     * */
    @Nullable
    protected final Reference<K, V> getReference(@Nullable Object key, Restructure restructure) {
        // 根据KEY计算hash
        int hash = getHash(key);
        // 根据hash找到对应段, 通过段找到对应节点
        return getSegmentForHash(hash).getReference(key, hash, restructure);
    }

    /**
     * 如果存在就覆盖
     * */
    @Override
    @Nullable
    public V put(@Nullable K key, @Nullable V value) {
        return put(key, value, true);
    }

    /**
     * 如果不存在,就放入Map.
     * */
    @Override
    @Nullable
    public V putIfAbsent(@Nullable K key, @Nullable V value) {
        return put(key, value, false);
    }

    /**
     * 给Map的指定Key设置新值, 返回旧值
     * @param key                键
     * @param value              新值
     * @param overwriteExisting  是否覆盖现有值
     * @return  旧值
     * */
    @Nullable
    private V put(@Nullable final K key, @Nullable final V value, final boolean overwriteExisting) {
        // 创建一个任务对象. 供doTask方法内执行使用
        return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.RESIZE) {
            @Override
            @Nullable
            protected V execute(@Nullable Reference<K, V> reference, @Nullable Entry<K, V> entry, @Nullable Entries entries) {
                if (entry != null) {
                    V oldValue = entry.getValue();
                    // 是否需要覆盖旧值, 根据传入值判断.
                    if (overwriteExisting) {
                        entry.setValue(value);
                    }
                    return oldValue;
                }
                Assert.state(entries != null, "No entries segment");
                entries.add(value);
                return null;
            }
        });
    }

    @Nullable
    private <T> T doTask(@Nullable Object key, Task<T> task) {
        int hash = getHash(key);
        // 调用对应段的任务执行器
        return getSegmentForHash(hash).doTask(hash, key, task);
    }

    /**
     * 通过hash获得段的索引值, 返回对应的段
     * 计算算法没有看懂
     * */
    private Segment getSegmentForHash(int hash) {
        return this.segments[(hash >>> (32 - this.shift)) & (this.segments.length - 1)];
    }

    /**
     * 计算可用于创建指定的最大值和最小值之间的2的幂的移位值
     * 每次循环左移一位, 即乘2
     * 判断两个传入值, 当value值比其中一个大的时候即跳出循环返回
     * @return  计算出的偏移
     * */
    protected static int calculateShift(int minimumValue, int maximumValue) {
        int shift = 0;
        int value = 1;
        while (value < minimumValue && value < maximumValue) {
            value <<= 1;
            shift++;
        }
        return shift;
    }

    /**
     * 该Map支持的各种参考类型
     * */
    public enum ReferenceType {
        /** Use {@link SoftReference}s */
        SOFT,
        /** Use {@link WeakReference}s */
        WEAK
    }

    /**
     * 单个段用于划分映射，以实现更好的并发性能
     * */
    @SuppressWarnings("serial")
    protected final class Segment extends ReentrantLock {

        private final ReferenceManager referenceManager;

        /**
         * 每个段的初始容量
         * */
        private final int initialSize;

        /**
         * 使用哈希中低序位索引的引用数组。
         * 此属性只能与{@code resizeThreshold}一起设置。
         * */
        private volatile Reference<K, V>[] references;

        /**
         * 该段包含的总数.这包括链接的引用和已被垃圾回收但尚未清除的引用
         * */
        private volatile int count = 0;

        /**
         * 调整参考大小时应达到阈值。当count超过此值时，引用将被调整大小
         * */
        private int resizeThreshold;

        public Segment(int initialCapacity) {
            this.referenceManager = createReferenceManager();
            // 根据Map初始容量计算段初始容量
            this.initialSize = 1 << calculateShift(initialCapacity, MAXIMUM_SEGMENT_SIZE);
            // 初始化引用数据数组, 创建指定数量个元素放入数组
            this.references = createReferenceArray(this.initialSize);
            // 拉取阈值负载因子, 并计算扩容阈值
            this.resizeThreshold = (int) (this.references.length * getLoadFactor());
        }

        @Nullable
        public Reference<K, V> getReference(@Nullable Object key, int hash, Restructure restructure) {
            if (restructure == Restructure.WHEN_NECESSARY) {
                restructureIfNecessary(false);
            }
            if (this.count == 0) {
                return null;
            }
            // 使用本地副本来防止其他线程写入
            Reference<K, V>[] references = this.references;
            // 获取索引
            int index = getIndex(hash, references);
            Reference<K, V> head = references[index];
            return findInChain(head, key, hash);
        }

        @Nullable
        public <T> T doTask(final int hash, @Nullable final Object key, final Task<T> task) {
            // 调用任务传参条件里是否有重新调整大小操作
            boolean resize = task.hasOption(TaskOption.RESIZE);
            // 调用任务传参条件里是否有前置操作, 判断是否需要重组数据
            if (task.hasOption(TaskOption.RESTRUCTURE_BEFORE)) {
                restructureIfNecessary(resize);
            }
            // 调用任务传参条件里是否有为空跳过, 同时没有数据
            if (task.hasOption(TaskOption.SKIP_IF_EMPTY) && this.count == 0) {
                // 执行空操作, 返回
                return task.execute(null, null, null);
            }
            lock();
            try {
                // 通过hash值求出索引
                final int index = getIndex(hash, this.references);
                // 获取链表头节点
                Reference<K, V> head = this.references[index];
                // 在链表中找到执行key节点
                Reference<K, V> reference = findInChain(head, key, hash);
                Entry<K, V> entry = (reference != null ? reference.get() : null);
                Entries entries = new Entries() {
                    @Override
                    public void add(@Nullable V value) {
                        @SuppressWarnings("unchecked")
                        // 创建一个新的Entry保存key和新的value
                        Entry<K, V> newEntry = new Entry<>((K) key, value);
                        // 创建成一个Reference. 根据类型创建成软引入或者是弱引入
                        Reference<K, V> newReference = Segment.this.referenceManager.createReference(newEntry, hash, head);
                        // 保存新的节点
                        Segment.this.references[index] = newReference;
                        // 总数加1
                        Segment.this.count++;
                    }
                };
                return task.execute(reference, entry, entries);
            } finally {
                unlock();
                // 调用任务传参条件里是否有后置操作, 判断是否需要重组数据
                if (task.hasOption(TaskOption.RESTRUCTURE_AFTER)) {
                    restructureIfNecessary(resize);
                }
            }
        }

        /**
         * 清空这个段的数据
         * */
        public void clear() {
            if (this.count == 0) {
                return;
            }
            lock();
            try {
                // 创建初始容量的数组, 填充初始化对象
                this.references = createReferenceArray(this.initialSize);
                // 计算原始扩容阈值
                this.resizeThreshold = (int) (this.references.length * getLoadFactor());
                // 计数归零
                this.count = 0;
            } finally {
                unlock();
            }
        }

        /**
         * 必要时重组基础数据结构。
         * 该方法可以增加引用表的大小，并清除所有垃圾回收的引用
         * */
        protected final void restructureIfNecessary(boolean allowResize) {
            boolean needsResize = ((this.count > 0) && (this.count >= this.resizeThreshold));
            // 取出队列节点
            Reference<K, V> reference = this.referenceManager.pollForPurge();
            if ((reference != null) || (needsResize && allowResize)) {
                lock();
                try {
                    int countAfterRestructure = this.count;

                    Set<Reference<K, V>> toPurge = Collections.emptySet();
                    // 循环迭代, 取出队列中所有节点, 放入集合中
                    if (reference != null) {
                        toPurge = new HashSet<>();
                        while (reference != null) {
                            toPurge.add(reference);
                            reference = this.referenceManager.pollForPurge();
                        }
                    }
                    countAfterRestructure -= toPurge.size();

                    // 重新计算，考虑到锁中的数量以及将要清除的元素
                    needsResize = (countAfterRestructure > 0 && countAfterRestructure >= this.resizeThreshold);
                    boolean resizing = false;
                    int restructureSize = this.references.length;
                    // 判断是否重新计算
                    if (allowResize && needsResize && restructureSize < MAXIMUM_SEGMENT_SIZE) {
                        restructureSize <<= 1;
                        resizing = true;
                    }
                    // 创建一个新表或重用现有表. 如果上述判断需要重新计算, 那么创建一个新表
                    Reference<K, V>[] restructured = resizing ? createReferenceArray(restructureSize) : this.references;

                    // 重组
                    for (int i = 0; i < this.references.length; i++) {
                        reference = this.references[i];
                        if (!resizing) {
                            restructured[i] = null;
                        }
                        while (reference != null) {
                            if (!toPurge.contains(reference)) {
                                Entry<K, V> entry = reference.get();
                                if (entry != null) {
                                    int index = getIndex(reference.getHash(), restructured);
                                    restructured[index] = this.referenceManager.createReference(entry, reference.getHash(), restructured[index]);
                                }
                            }
                            reference = reference.getNext();
                        }
                    }

                    // 替换volatile数据
                    if (resizing) {
                        this.references = restructured;
                        this.resizeThreshold = (int) (this.references.length * getLoadFactor());
                    }
                    this.count = Math.max(countAfterRestructure, 0);
                } finally {
                    unlock();
                }
            }
        }

        /**
         * 在链表中查找指定节点
         * 先比较节点hash值, 再取出节点内的key值进行比较
         * */
        private Reference<K, V> findInChain(Reference<K, V> reference, @Nullable Object key, int hash) {
            Reference<K, V> currRef = reference;
            // 循环链表
            while (currRef != null) {
                // 找到hash值相同的节点
                if (currRef.getHash() == hash) {
                    Entry<K, V> entry = currRef.get();
                    if (entry != null) {
                        K entryKey = entry.getKey();
                        // 判断key是否相等, 相等即返回该节点
                        if (ObjectUtils.nullSafeEquals(entryKey, key)) {
                            return currRef;
                        }
                    }
                }
                // 没找到, 继续处理链表下一个节点
                currRef = currRef.getNext();
            }
            return null;
        }

        /**
         * 根据传入数量创建指定数量的Reference对象, 填入references数组中
         * */
        @SuppressWarnings("unchecked")
        private Reference<K, V>[] createReferenceArray(int size) {
            return (Reference<K, V>[]) Array.newInstance(Reference.class, size);
        }

        /**
         * 根据hash计算数组索引
         * */
        public int getIndex(int hash, Reference<K, V>[] references) {
            return (hash & (references.length - 1));
        }

        /**
         * 返回当前引用数组的大小
         * */
        public final int getSize() {
            return this.references.length;
        }

        /**
         * 返回此段中的引用总数
         * */
        public final int getCount() {
            return this.count;
        }
    }

    /**
     * 对Map中包含的{@link Entry}的引用。实现通常是特定Java参考实现的包装器
     * 引用类, 一个接口, 实际调用其两个实现类, 弱引用和软引用. 这里只是这两种引用类的通用方法接口
     * Reference是单向链表结构
     * */
    protected interface Reference<K, V> {
        /**
         * 返回引用的条目；如果条目不再可用，则返回
         * @return  引用的条目
         */
        @Nullable
        Entry<K, V> get();

        /**
         * 返回哈希值以供参考
         * @return  hash值
         * */
        int getHash();

        /**
         * 返回链中的下一个引用；如果没有，则返回null
         * @return  链表中下一个引用
         */
        Reference<K, V> getNext();

        /**
         * 释放此条目，并确保它将从{@code ReferenceManager#pollForPurge()}返回。
         */
        @Nullable
        void release();
    }

    protected static final class Entry<K, V> implements Map.Entry<K, V> {

        @Nullable
        private final K key;

        @Nullable
        private volatile V value;

        public Entry(@Nullable K key,@Nullable V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        @Nullable
        public K getKey() {
            return this.key;
        }

        @Override
        @Nullable
        public V getValue() {
            return this.value;
        }

        @Override
        @Nullable
        public V setValue(V value) {
            // 保存旧值
            V previous = this.value;
            // 赋值新值
            this.value = value;
            // 返回旧值
            return previous;
        }

        @Override
        public String toString() {
            return (this.key + "=" + this.value);
        }

        @Override
        public final boolean equals(Object other) {
            // == 判断是否是一个内存地址.
            if (this == other) {
                return true;
            }
            // 是否派生同一个父类
            if (!(other instanceof Map.Entry)) {
                return false;
            }
            Map.Entry otherEntry = (Map.Entry) other;
            // 调用对象工具比对key和value是否相等
            return (ObjectUtils.nullSafeEquals(getKey(), otherEntry.getKey()) && ObjectUtils.nullSafeEquals(getValue(), otherEntry.getValue()));
        }

        @Override
        public final int hashCode() {
            return ObjectUtils.nullSafeHashCode(this.key) ^ ObjectUtils.nullSafeHashCode(this.value);
        }
    }

    /**
     * 可以针对{@link Segment}进行{@link Segment#doTask run}的任务
     * */
    private abstract class Task<T> {
        private final EnumSet<TaskOption> options;

        public Task(TaskOption... options) {
            // 创建一个空的枚举集或创建一个包含第一个枚举值的新的枚举集
            this.options = (options.length == 0 ? EnumSet.noneOf(TaskOption.class) : EnumSet.of(options[0], options));
        }

        public boolean hasOption(TaskOption option) {
            return this.options.contains(option);
        }

        /**
         * 执行任务
         * */
        protected T execute(@Nullable Reference<K, V> reference, @Nullable Entry<K, V> entry, @Nullable Entries entries) {
            return execute(reference, entry);
        }

        /**
         * 可用于不需要访问{@link Entries}的任务的便捷方法
         * */
        @Nullable
        protected T execute(@Nullable Reference<K, V> reference, @Nullable Entry<K, V> entry) {
            return null;
        }
    }

    /**
     * Task支持的各种选项
     * */
    private enum TaskOption {
        /**
         * 重组前
         * */
        RESTRUCTURE_BEFORE,
        /**
         * 重组后
         * */
        RESTRUCTURE_AFTER,
        /**
         * 如果为空则跳过
         * */
        SKIP_IF_EMPTY,
        /**
         * 调整大小
         * */
        RESIZE
    }

    /**
     * 允许任务访问{@link Segment}条目。
     * */
    private abstract class Entries {
        /**
         * 添加具有指定值的新条目
         * @param   value  要添加的值
         * */
        public abstract void add(@Nullable V value);
    }

    /**
     * 内部Entry集合实现
     * */
    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            // 判断是否同个父类
            if (o != null && o instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) o;
                // 获取对应节点
                Reference<K, V> reference = ConcurrentReferenceHashMap.this.getReference(entry.getKey(), Restructure.NEVER);
                Entry<K, V> other = (reference != null ? reference.get() : null);
                // 节点存在, 判断节点内的值
                if (other != null) {
                    return ObjectUtils.nullSafeEquals(entry.getValue(), other.getValue());
                }
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {

        }

        @Override
        public int size() {
            return ConcurrentReferenceHashMap.this.size();
        }

        @Override
        public void clear() {
            ConcurrentReferenceHashMap.this.clear();
        }
    }

    /**
     * 内部数据迭代器实现
     * */
    private class EntryIterator implements Iterator<Map.Entry<K, V>> {
        /**
         * 段的序列
         * */
        private int segmentIndex;

        /**
         * 段中节点序列
         * */
        private int referenceIndex;

        @Nullable
        private Reference<K, V>[] references;

        @Nullable
        private Reference<K, V> reference;

        @Nullable
        private Entry<K, V> next;

        @Nullable
        private Entry<K, V> last;

        public EntryIterator() {
            moveToNextSegment();
        }

        @Override
        public boolean hasNext() {
            getNextIfNecessary();
            return (this.next != null);
        }

        /**
         * 下一个节点
         * */
        @Override
        public Entry<K, V> next() {
            getNextIfNecessary();
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            this.last = this.next;
            this.next = null;
            return this.last;
        }

        /**
         * 向下一个节点移动
         * */
        private void getNextIfNecessary() {
            while (this.next == null) {
                moveToNextReference();
                if (this.reference == null) {
                    return;
                }
                this.next = this.reference.get();
            }
        }

        /**
         * 移动到下一个节点
         * */
        private void moveToNextReference() {
            if (reference != null) {
                this.reference = this.reference.getNext();
            }
            while (this.reference == null && this.references != null) {
                // 如果已经到该段的最后一个节点
                if (this.referenceIndex >= this.references.length) {
                    moveToNextSegment();
                    // 初始化节点索引为0
                    this.referenceIndex = 0;
                } else {
                    this.reference = this.references[this.referenceIndex];
                    this.referenceIndex++;
                }
            }
        }

        /**
         * 移动到下一个段
         * */
        private void moveToNextSegment() {
            this.reference = null;
            this.references = null;
            // 不是最后一个段
            if (this.segmentIndex < ConcurrentReferenceHashMap.this.segments.length) {
                // 取出下一个段的数据
                this.references = ConcurrentReferenceHashMap.this.segments[this.segmentIndex].references;
                // 段索引值递增1
                this.segmentIndex++;
            }
        }

        @Override
        public void remove() {
            Assert.state(this.last != null, "No element to remove");
            ConcurrentReferenceHashMap.this.remove(this.last.getKey());
        }
    }

    /**
     * 可以执行的重组类型
     * */
    protected enum Restructure {
        /**
         * 必要时
         * */
        WHEN_NECESSARY,
        /**
         * 从不
         * */
        NEVER
    }

    /**
     * 用于管理Reference的策略类。如果需要支持替代引用类型，则可以覆盖此类。
     * */
    protected class ReferenceManager {
        private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<>();

        /**
         * 用于创建新工厂的工厂方法
         * @param   entry    Reference中包含的条目
         * @param   hash     hash值
         * @param   next     链表中下一个引用
         * */
        public Reference<K, V> createReference(Entry<K, V> entry, int hash, @Nullable Reference<K, V> next) {
            if (ConcurrentReferenceHashMap.this.referenceType == ReferenceType.WEAK) {
                return new WeakEntryReference<>(entry, hash, next, this.queue);
            }
            return new SoftEntryReference<>(entry, hash, next, this.queue);
        }

        /**
         * 返回已被垃圾回收并且可以从基础结构中清除的任何引用；
         * 如果不需要清除引用，则可以返回null。
         * 该方法必须是线程安全的，并且在返回null时理想情况下不应阻塞。
         * 引用应仅返回一次。
         * */
        @SuppressWarnings("unchecked")
        @Nullable
        public Reference<K, V> pollForPurge() {
            return (Reference<K, V>) this.queue.poll();
        }
    }

    /**
     * SoftReference 的内部实现
     * */
    private static final class SoftEntryReference<K, V> extends SoftReference<Entry<K, V>> implements Reference<K, V> {
        private final int hash;

        @Nullable
        private final Reference<K, V> nextReference;

        public SoftEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
            super(entry, queue);
            this.hash = hash;
            this.nextReference = next;
        }

        @Override
        public int getHash() {
            return this.hash;
        }

        @Override
        @Nullable
        public Reference<K, V> getNext() {
            return this.nextReference;
        }

        @Override
        public void release() {
            enqueue();
            clear();
        }
    }

    /**
     * WeakReference 的内部实现
     * */
    private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {

        private final int hash;

        @Nullable
        private final Reference<K, V> nextReference;

        public WeakEntryReference(Entry<K, V> entry, int hash, @Nullable Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
            super(entry, queue);
            this.hash = hash;
            this.nextReference = next;
        }

        @Override
        public int getHash() {
            return this.hash;
        }

        @Override
        @Nullable
        public Reference<K, V> getNext() {
            return this.nextReference;
        }

        @Override
        public void release() {
            enqueue();
            clear();
        }
    }
}
