package io.elastest.eus.test;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;

import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.service.EusJsonService;
import io.elastest.eus.test.util.WebSocketClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "io.reflectoring.scheduling.enabled=false")
public class IntegrationBaseTest {
    protected final Logger log = getLogger(lookup().lookupClass());

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected EusJsonService jsonService;

    @LocalServerPort
    protected int serverPort;

    @Value("${ws.path}")
    protected String wsPath;

    @Value("${api.context.path}")
    protected String apiContextPath;

    protected WebSocketClient createWebSocket() {
        String wsUrl = "ws://localhost:" + serverPort + wsPath;
        log.debug("Websocket url: {}", wsUrl);
        return new WebSocketClient(wsUrl);
    }

    protected ResponseEntity<String> getBrowsersResponse() {
        return restTemplate.getForEntity(apiContextPath + "/browsers",
                String.class);
    }

    protected ResponseEntity<String> startSession(String browserName,
            String browserVersion) {
        String jsonMessage = "{\"capabilities\":{\"alwaysMatch\":{\"acceptInsecureCerts\":true},"
                + "\"desiredCapabilities\":{\"acceptInsecureCerts\":true,\"browserName\":\""
                + browserName + "\"," + "\"platform\":\"ANY\","
                + "\"version\":\"" + browserVersion
                + "\",\"loggingPrefs\":{\"browser\":\"ALL\"}},"
                + "\"firstMatch\":[{\"browserName\":\"" + browserName
                + "\"}],\"requiredCapabilities\":{}},"
                + "\"desiredCapabilities\":{\"acceptInsecureCerts\":true,\"browserName\":\""
                + browserName + "\"," + "\"platform\":\"ANY\",\"version\":\""
                + browserVersion + "\",\"loggingPrefs\":{\"browser\":\"ALL\"}},"
                + "\"requiredCapabilities\":{}}";
        return restTemplate.postForEntity(apiContextPath + "/session",
                jsonMessage, String.class);
    }

    protected void deleteSession(String sessionId) {
        restTemplate.delete(apiContextPath + "/session/" + sessionId);
    }

    protected String getSessionIdFromResponse(ResponseEntity<String> response)
            throws IOException {
        String responseBody = response.getBody();
        return jsonService
                .jsonToObject(responseBody, WebDriverSessionResponse.class)
                .getSessionId();
    }

    protected ResponseEntity<String> getVncSession(String sessionId) {
        return restTemplate.getForEntity(
                apiContextPath + "/session/" + sessionId + "/vnc",
                String.class);
    }

    protected ResponseEntity<String> getRecordings(String sessionId) {
        return restTemplate.getForEntity(
                apiContextPath + "/session/" + sessionId + "/recording",
                String.class);
    }

    protected void deleteSessionRecordings(String sessionId) {
        restTemplate.delete(
                apiContextPath + "/session/" + sessionId + "/recording");
    }
    
//    protected ResponseEntity<String> uploadFileToSession(){
//        
//    }

}
