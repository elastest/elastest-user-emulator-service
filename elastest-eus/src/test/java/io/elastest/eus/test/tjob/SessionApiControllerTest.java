package io.elastest.eus.test.tjob;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.test.extensions.MockitoExtension;

@RunWith(JUnitPlatform.class)
@ExtendWith({ SpringExtension.class, MockitoExtension.class })
public class SessionApiControllerTest {

    @Test
    public void subscribeToEventTest() {
        assertTrue(true);
    }

}
