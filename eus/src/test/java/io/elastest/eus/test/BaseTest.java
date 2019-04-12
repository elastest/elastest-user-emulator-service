package io.elastest.eus.test;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.service.EusJsonService;
import io.elastest.eus.test.util.WebSocketClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "io.reflectoring.scheduling.enabled=false")
public class BaseTest {
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

    @Value("${container.shared.files.folder}")
    protected String containerSharedFilesFolder;

    @Value("${ws.protocol.getSessions}")
    protected String wsProtocolGetSessions;

    @Value("${ws.protocol.getRecordings}")
    protected String wsProtocolGetRecordings;

    @Value("${ws.protocol.newSession}")
    protected String wsProtocolNewSession;

    @Value("${ws.protocol.recordedSession}")
    protected String wsProtocolRecordedSesssion;

    @Value("${registry.metadata.extension}")
    protected String registryMetadataExtension;

    @Value("${registry.folder}")
    protected String registryFolder;

    protected String eusUrl;
    protected String wsUrl;

    @BeforeEach
    public void setupTest(TestInfo info) throws MalformedURLException {
        eusUrl = "http://localhost:" + serverPort + apiContextPath;
        wsUrl = "ws://localhost:" + serverPort + wsPath;

        String testName = info.getTestMethod().get().getName();
        log.info("##### Start test: {}", testName);
    }

    @AfterEach
    public void teardown(TestInfo info) {
        String testName = info.getTestMethod().get().getName();
        log.info("##### Finish test: {}", testName);
    }

    protected WebSocketClient createWebSocket() {
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

    protected ResponseEntity<String> uploadFileToSession(MultipartFile file,
            String sessionId) throws IOException {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        ByteArrayResource contentsAsResource = new ByteArrayResource(
                IOUtils.toByteArray(file.getInputStream())) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        };
        map.add("file", contentsAsResource);

        return restTemplate.postForEntity(
                apiContextPath + "/browserfile/session/" + sessionId, map,
                String.class);
    }

    protected MultipartFile getMultipartFileFromString(String name,
            String content) {
        return new MockMultipartFile(name, name, null, content.getBytes());
    }

    protected ResponseEntity<InputStreamResource> getFileFromSession(
            String completePath, String sessionId) {
        return restTemplate.getForEntity(
                apiContextPath + "/browserfile/session/" + sessionId + "/"
                        + completePath + "?isDirectory=false",
                InputStreamResource.class);
    }

    protected ResponseEntity<InputStreamResource> getUploadedFileFromSession(
            String fileName, String sessionId) {
        String completeFilePath = containerSharedFilesFolder + "/" + fileName;
        log.debug("Getting file {} from session {}", completeFilePath,
                sessionId);
        return getFileFromSession(completeFilePath, sessionId);
    }

}
