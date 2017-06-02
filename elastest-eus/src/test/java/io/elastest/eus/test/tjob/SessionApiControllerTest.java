package io.elastest.eus.test.tjob;


import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.api.SessionApiController;
import io.elastest.eus.api.model.Event;
import io.elastest.eus.test.extensions.MockitoExtension;


@RunWith(JUnitPlatform.class)
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class SessionApiControllerTest {
	
	private SessionApiController sessionApiController;

	
	
	@Test
	public void subscribeToEventTest(){
		assertTrue(true);	
        
		
	}
	
	
}
