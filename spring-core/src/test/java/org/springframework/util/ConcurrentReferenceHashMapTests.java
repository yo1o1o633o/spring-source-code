package org.springframework.util;


import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ConcurrentReferenceHashMapTests {

    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldCreateWithDefaults() {
        ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
        assertThat(map.getSegmentsSize(), is(16));
        assertThat(map.getSegment(0).getSize(), is(1));
        assertThat(map.getLoadFactor(), is(0.75f));
    }

    @Test
    public void shouldCreateWithInitialCapacity() {
        ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
        assertThat(map.getSegmentsSize(), is(16));
        assertThat(map.getSegment(0).getSize(), is(2));
        assertThat(map.getLoadFactor(), is(0.75f));
    }

    @Test
    public void shouldCreateWithInitialCapacityAndLoadFactor() {
        ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
        assertThat(map.getSegmentsSize(), is(16));
        assertThat(map.getSegment(0).getSize(), is(2));
        assertThat(map.getLoadFactor(), is(0.5f));
    }

    @Test
    public void shouldCreateWithInitialCapacityAndConcurrenyLevel() {
        ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
        assertThat(map.getSegmentsSize(), is(2));
        assertThat(map.getSegment(0).getSize(), is(8));
        assertThat(map.getLoadFactor(), is(0.5f));
    }

    @Test
    public void shouldCreateFullyCustom() {
        ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
        // 并发级别3最终为4（最近幂为2）
        assertThat(map.getSegmentsSize(), is(4));
        // initialCapacity为5/4（四舍五入至最接近的2的幂）
        assertThat(map.getSegment(0).getSize(), is(2));
        assertThat(map.getLoadFactor(), is(0.5f));
    }

    public void shouldNeedNonNegativeInitialCapacity() {
        new ConcurrentReferenceHashMap<Integer, String>(0, 1);
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("Initial capacity must not be negative");
    }


    private static class TestWeakConcurrentCache<K, V> extends ConcurrentReferenceHashMap<K, V> {

    }
}
