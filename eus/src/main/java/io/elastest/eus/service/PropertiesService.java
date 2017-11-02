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
package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service implementation for properties.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class PropertiesService {

    final Logger log = getLogger(lookup().lookupClass());

    private Map<String, Map<String, String>> browsers = new TreeMap<>(
            Collections.reverseOrder());

    @Value("${properties.filename}")
    private String propertiesFilename;

    @Value("${properties.separator.char}")
    private String propertiesSeparatorChar;

    @Value("${webdriver.any.platform}")
    private String webdriverAnyPlatform;

    @Value("${webdriver.browserName}")
    private String webdriverBrowserName;

    @Value("${webdriver.version}")
    private String webdriverVersion;

    @Value("${webdriver.platform}")
    private String webdriverPlatform;

    @Value("${properties.docker.image.key}")
    private String propertiesDockerImageKey;

    @PostConstruct
    private void postConstruct() throws IOException {
        log.debug("Getting existing browsers from {}", propertiesFilename);
        Properties properties = new Properties();

        try (final InputStream stream = this.getClass().getClassLoader()
                .getResourceAsStream(propertiesFilename)) {

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

        String[] split = key.split(propertiesSeparatorChar);
        entry.put(webdriverBrowserName, split[0]);
        entry.put(webdriverVersion, split[1]);
        entry.put(webdriverPlatform, split[2]);
        entry.put(propertiesDockerImageKey, dockerImage);

        return entry;
    }

    public String getDockerImageFromKey(String key) {
        return browsers.get(key).get(propertiesDockerImageKey);
    }

    public String getDockerImageFromCapabilities(String browserName,
            String version, String platform) {
        String key = getKeyFromCapabilities(browserName, version, platform);
        return getDockerImageFromKey(key);
    }

    public String getKeyFromCapabilities(String browserName, String version,
            String platform) {
        assert (browserName != null);
        log.debug("Capabilities: browserName={}, version={}, platform={}",
                browserName, version, platform);

        //Transform 61.0 in 61
        version = version.replace(".0", "");
        
        String out = null;
        for (String key : browsers.keySet()) {
            if (key.contains(browserName)) {
                if (version != null) {
                    if (key.contains(version) && platformMatch(key, platform)) {
                        out = key;
                    }
                } else if (platformMatch(key, platform)) {
                    out = key;
                }
                if (out != null) {
                    break;
                }
            }
        }
        return out;
    }

    public boolean platformMatch(String key, String platform) {
        return (platform != null && key.contains(platform)) || platform == null
                || platform.equals("")
                || platform.equalsIgnoreCase(webdriverAnyPlatform);
    }

    public String getVersionFromKey(String key) {
        return key.split(propertiesSeparatorChar)[1];
    }

}
