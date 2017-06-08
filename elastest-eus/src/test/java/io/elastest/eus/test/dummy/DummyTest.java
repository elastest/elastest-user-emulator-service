package io.elastest.eus.test.dummy;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
public class DummyTest {

    @Test
    public void test() {
        System.out.println("*** This is a dummy test ***");
        assertTrue(true);
    }

}
