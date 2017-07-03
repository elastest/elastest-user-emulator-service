/*
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.elastest.eus.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.elastest.eus.api.EusException;

/**
 * Service implementation for properties.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class PropertiesService {

    private final Logger log = LoggerFactory.getLogger(PropertiesService.class);

    private Map<String, Map<String, String>> browsers = new TreeMap<>(
            Collections.reverseOrder());

    private static final String DOCKER_BROWSER_FILENAME = "docker-browser.properties";

    private static final String SEPARATOR_CHAR = "_";

    private static final String BROWSERNAME_KEY = "browserName";
    private static final String VERSON_KEY = "version";
    private static final String PLATFORM_KEY = "platform";
    private static final String DOCKER_IMAGE = "dockerImage";

    @PostConstruct
    public void postConstruct() throws IOException {
        log.debug("Getting existing browsers from {}", DOCKER_BROWSER_FILENAME);
        Properties properties = new Properties();

        try (final InputStream stream = this.getClass().getClassLoader()
                .getResourceAsStream(DOCKER_BROWSER_FILENAME)) {

            properties.load(stream);

            Set<Object> keySet = properties.keySet();
            for (Object key : keySet) {
                log.trace("{} {}", key, properties.get(key));
                browsers.put((String) key, createEntryFromKey((String) key,
                        (String) properties.get(key)));
            }

        }
    }

    private Map<String, String> createEntryFromKey(String key,
            String dockerImage) {
        Map<String, String> entry = new HashMap<>();

        String[] split = ((String) key).split(SEPARATOR_CHAR);
        entry.put(BROWSERNAME_KEY, split[0]);
        entry.put(VERSON_KEY, split[1]);
        entry.put(PLATFORM_KEY, split[2]);
        entry.put(DOCKER_IMAGE, dockerImage);

        return entry;
    }

    public String getDockerImageFromJson(String jsonMessage) {
        String key = getKeyFromJson(jsonMessage);
        return browsers.get(key).get(DOCKER_IMAGE);
    }

    public String getDockerImageFromCapabilities(String browserName,
            String version, String platform) {
        String key = getKeyFromCapabilities(browserName, version, platform);
        return browsers.get(key).get(DOCKER_IMAGE);
    }

    public String getKeyFromJson(String jsonMessage) {
        JSONObject jsonObj = new JSONObject(jsonMessage);
        JSONObject desiredCapabilities = (JSONObject) jsonObj
                .get("desiredCapabilities");

        String browserName = (String) desiredCapabilities.get("browserName");
        String version = (String) desiredCapabilities.get("version");
        String platform = (String) desiredCapabilities.get("platform");

        String keyFromCapabilities = getKeyFromCapabilities(browserName,
                version, platform);

        log.debug("keyFromCapabilities={}", keyFromCapabilities);
        return keyFromCapabilities;
    }

    public String getKeyFromCapabilities(String browserName, String version,
            String platform) {

        if (browserName == null) {
            throw new EusException(
                    "At least the type of browser must be honored");
        }

        log.debug("browserName={}, version={}, platform={}", browserName,
                version, platform);

        String out = null;
        for (String key : browsers.keySet()) {
            if (key.contains(browserName) && version != null
                    && key.contains(version) && platform != null
                    && key.contains(platform)) {
                out = key;
            } else if (key.contains(browserName) && version != null
                    && key.contains(version) && platform == null) {
                out = key;
            } else if (key.contains(browserName) && version == null
                    && platform != null && key.contains(platform)) {
                out = key;
            } else if (key.contains(browserName) && version == null
                    && platform == null) {
                out = key;
            }
            if (out != null) {
                break;
            }
        }

        return out;
    }

}
