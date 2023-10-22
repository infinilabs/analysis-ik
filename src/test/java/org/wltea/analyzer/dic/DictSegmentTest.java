package org.wltea.analyzer.dic;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DictSegmentTest {

    private DictSegment dictSegmentUnderTest;

    @Before
    public void setUp() {
        dictSegmentUnderTest = new DictSegment('a');
    }

    @Test
    public void testValueOf() {
        //127及以前的是缓存的同一个对象
        for (int i = Character.MIN_VALUE; i <= 127; i++) {
            assertTrue(Character.valueOf((char)i) == DictSegment.valueOf((char)i));
        }
        //127以后是各自生成自己的对象
        for (int i = 128; i <= Character.MAX_VALUE; i++) {
            assertFalse(Character.valueOf((char)i) == DictSegment.valueOf((char)i));
            assertEquals(Character.valueOf((char)i), DictSegment.valueOf((char)i));
        }
    }

    @Test
    public void testHasNextNode() {
        assertFalse(dictSegmentUnderTest.hasNextNode());
    }

    @Test
    public void testMatch1() {
        // Setup
        // Run the test
        final Hit result = dictSegmentUnderTest.match(new char[]{'a'});

        // Verify the results
    }

    @Test
    public void testMatch2() {
        // Setup
        // Run the test
        final Hit result = dictSegmentUnderTest.match(new char[]{'a'}, 0, 0);

        // Verify the results
    }

    @Test
    public void testMatch3() {
        // Setup
        final Hit searchHit = new Hit();
        searchHit.setMatchedDictSegment(new DictSegment('a'));
        searchHit.setBegin(0);
        searchHit.setEnd(0);

        // Run the test
        final Hit result = dictSegmentUnderTest.match(new char[]{'a'}, 0, 0, searchHit);

        // Verify the results
    }

    @Test
    public void testFillSegment1() {
        // Setup
        // Run the test
        dictSegmentUnderTest.fillSegment(new char[]{'a'});

        // Verify the results
    }

    @Test
    public void testDisableSegment() {
        // Setup
        // Run the test
        dictSegmentUnderTest.disableSegment(new char[]{'a'});

        // Verify the results
    }

    @Test
    public void testCompareTo() {
        // Setup
        final DictSegment o = new DictSegment('a');

        // Run the test
        final int result = dictSegmentUnderTest.compareTo(o);

        // Verify the results
        assertEquals(0, result);
    }

    @Test(expected = NullPointerException.class)
    public void testCompareTo_ThrowsNullPointerException() {
        // Setup
        final DictSegment o = new DictSegment('a');

        // Run the test
        dictSegmentUnderTest.compareTo(o);
    }

    @Test(expected = ClassCastException.class)
    public void testCompareTo_ThrowsClassCastException() {
        // Setup
        final DictSegment o = new DictSegment('a');

        // Run the test
        dictSegmentUnderTest.compareTo(o);
    }
}
