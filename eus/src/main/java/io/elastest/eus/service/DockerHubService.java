/*
 * (C) Copyright 2018 Boni Garcia (http://bonigarcia.github.io/)
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

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.eus.EusException;
import io.elastest.eus.json.DockerHubApi;
import io.elastest.eus.json.DockerHubTags;
import io.elastest.eus.json.DockerHubTags.DockerHubTag;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Docker Hub service.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
@Service
public class DockerHubService {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${docker.hub.url}")
    String dockerHubUrl;

    @Value("${browser.image.format}")
    String browserImageFormat;

    @Value("${browser.docker.hub.timeout}")
    int browserDockerHubTimeout;

    DockerHubApi dockerHubApi;

    private void initDockerHubApi() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(browserDockerHubTimeout, SECONDS)
                .readTimeout(browserDockerHubTimeout, SECONDS).build();
        Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(dockerHubUrl).build();
        dockerHubApi = retrofit.create(DockerHubApi.class);
    }

    private List<DockerHubTag> listTags() throws IOException {
        if (dockerHubApi == null) {
            initDockerHubApi();
        }

        log.debug("Getting browser image list from Docker Hub: {}",
                dockerHubUrl);
        Response<DockerHubTags> listTagsResponse = dockerHubApi.listTags()
                .execute();
        if (!listTagsResponse.isSuccessful()) {
            throw new EusException(listTagsResponse.errorBody().string());
        }
        return listTagsResponse.body().getResults();
    }

    private int compareVersions(String v1, String v2) {
        String[] v1split = v1.split("\\.");
        String[] v2split = v2.split("\\.");
        int length = max(v1split.length, v2split.length);
        for (int i = 0; i < length; i++) {
            int v1Part = i < v1split.length ? parseInt(v1split[i]) : 0;
            int v2Part = i < v2split.length ? parseInt(v2split[i]) : 0;
            if (v1Part < v2Part) {
                return 1;
            }
            if (v1Part > v2Part) {
                return -1;
            }
        }
        return 0;
    }

    private String getVersionFromList(List<String> browserList,
            String version) {
        if (version == null || version.isEmpty()) {
            return browserList.get(0);
        }

        if (browserList.contains(version)) {
            return version;
        }

        for (String v : browserList) {
            if (v.startsWith(version)) {
                return v;
            }
        }

        throw new EusException(
                "Version " + version + " is not valid for browser");
    }

    public String getBrowserImageFromCapabilities(String browser,
            String version, String platform) throws IOException {
        log.debug(
                "Getting browser image from capabilities: browser={} version={} platform={}",
                browser, version, platform);
        List<DockerHubTag> tagList = listTags();
        log.trace("Selenoid browser tag list: {}", tagList);

        String imagePreffix = browser + "_";
        List<String> browserList = tagList.stream()
                .filter(p -> p.getName().startsWith(browser))
                .map(p -> p.getName().replace(imagePreffix, ""))
                .sorted(this::compareVersions).collect(toList());
        log.trace("Browser list for {}: {}", browser, browserList);

        return format(browserImageFormat, browser,
                getVersionFromList(browserList, version));
    }

    public String getVersionFromImage(String image) {
        return image.substring(image.indexOf('_') + 1);
    }

    public Map<String, List<String>> getBrowsers() throws IOException {
        Map<String, List<String>> result = new TreeMap<>();
        for (DockerHubTag dockerHubTag : listTags()) {
            String tagName = dockerHubTag.getName();
            String browser = tagName.substring(0, tagName.indexOf('_'));
            String version = tagName.substring(tagName.indexOf('_') + 1);

            if (browser.equalsIgnoreCase("opera")
                    && version.equalsIgnoreCase("12.16")) {
                continue;
            }

            if (result.containsKey(browser)) {
                List<String> list = result.get(browser);
                list.add(version);
            } else {
                List<String> entry = new ArrayList<>();
                entry.add(version);
                result.put(browser, entry);
            }
        }

        for (List<String> list : result.values()) {
            Collections.sort(list, this::compareVersions);
        }

        return result;
    }

}
