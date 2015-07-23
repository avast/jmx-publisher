package com.avast.cloudutils.jmx.test;

import org.junit.Test;

import java.io.IOException;

/**
 * to test annotation inheritance
 *
 * Created by jacob on 6/25/15.
 */
public class LevelPropertyTest {

    @Test
    public void testParentProperty() throws IOException {
        Sub1Class s1 = new Sub1Class();
//        Sub2Class s2 = new Sub2Class();

        System.out.printf("test");
    }
}
