package org.praxislive.core.types;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * 
 */
public class PMapTest {

    public PMapTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of coerce method, of class PMap.
     */
    @Test
    public void testCoerce() throws Exception {
        PMap m = PMap.of("template", "public void draw(){");
        String mStr = m.toString();
        System.out.println(mStr);
        PMap m2 = PMap.parse(mStr);
        assertTrue(Utils.equivalent(m, m2));
    }

    @Test
    public void testParse() throws Exception {
        PMap m1 = PMap.builder()
                .put("key1", "value1")
                .put("key2", 2)
                .put("key3", true)
                .put("key4", 13.66)
                .build();
        PMap m2 = PMap.parse("key1 value1 key2 2 key3 true key4 13.66");
        PMap m3 = PMap.parse(
                "key1 value1;\n"
                + "key2 2  \n"
                + "key3\ttrue\n"
                + "# this is a comment \n"
                + "key4 {13.66}"
        );
        assertTrue(m1.equivalent(m2));
        assertTrue(m2.equivalent(m3));
        assertEquals(13.66, m3.getDouble("key4", 0), 0.001);
    }
    
    @Test
    public void testMerge() {
        PMap base = PMap.of(
                "key1", true,
                "key2", "",
                "key3", 25,
                "key4", "Still here"
        );
        PMap additional = PMap.of(
                "key2", "Replaced",
                "key4", "Not me",
                "key5", "Additional"
        );
        PMap result = PMap.merge(base, additional, PMap.IF_ABSENT);
        assertEquals(5, result.size());
        assertEquals("Still here", result.get("key4").toString());
        assertEquals("Additional", result.get("key5").toString());
        
        additional = PMap.of(
                "key4", "",
                "key3", 42
        );
        
        result = PMap.merge(base, additional, PMap.REPLACE);
        assertEquals(List.of("key1", "key2", "key3"), result.keys());
        assertEquals(42, result.getInt("key3", 0));
        assertNull(result.get("key4"));
        
        result = PMap.merge(base, PMap.EMPTY, PMap.IF_ABSENT);
        assertSame(base, result);
        
    }

}
