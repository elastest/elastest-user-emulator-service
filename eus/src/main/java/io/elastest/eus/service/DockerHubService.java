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
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.eus.EusException;
import io.elastest.eus.json.DockerHubApi;
import io.elastest.eus.json.DockerHubNameSpaceImages;
import io.elastest.eus.json.DockerHubNameSpaceImages.DockerHubNameSpaceImage;
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

    @Value("${browser.image.namespace}")
    String browserImageNamespace;

    @Value("${browser.docker.hub.timeout}")
    int browserDockerHubTimeout;

    @Value("${browser.image.skip.prefix}")
    String browserImageSkipPrefix;

    @Value("${browser.image.latest.version}")
    String browserImageLatestVersion;

    @Value("${et.internet.disabled}")
    boolean etInternetDisabled;

    Map<String, List<String>> cachedAvailableBrowsers = new TreeMap<>();

    DockerHubApi dockerHubApi;

    @PostConstruct
    void init() {
        try {
            getBrowsers(false);
        } catch (IOException e) {
            log.error("Error on get browsers on startup");
        }
    }

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

    private List<DockerHubNameSpaceImage> listImages() throws IOException {
        if (dockerHubApi == null) {
            initDockerHubApi();
        }

        log.debug("Getting browser image list from Docker Hub: {}",
                dockerHubUrl);
        Response<DockerHubNameSpaceImages> listImagesResponse = dockerHubApi
                .listImages(browserImageNamespace).execute();

        if (!listImagesResponse.isSuccessful()) {
            throw new EusException(listImagesResponse.errorBody().string());
        }
        return listImagesResponse.body().getResults();
    }

    private List<DockerHubTag> listTags(String browserImage)
            throws IOException {
        if (dockerHubApi == null) {
            initDockerHubApi();
        }

        log.debug("Getting browser {} version list from Docker Hub: {}",
                browserImage, dockerHubUrl);
        Response<DockerHubTags> listTagsResponse = dockerHubApi
                .listTags(browserImage).execute();
        if (!listTagsResponse.isSuccessful()) {
            throw new EusException(listTagsResponse.errorBody().string());
        }
        return listTagsResponse.body().getResults();
    }

    private int compareVersions(String v1, String v2) {
        if (this.isNumericVersion(v1) && this.isNumericVersion(v2)) {
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
        } else if (this.isNumericVersion(v1) && !this.isNumericVersion(v2)) {
            if (browserImageLatestVersion.equals(v2)) {
                return 1;
            } else {
                return -1;
            }
        } else if (!this.isNumericVersion(v1) && this.isNumericVersion(v2)) {
            if (browserImageLatestVersion.equals(v1)) {
                return -1;
            } else {
                return 1;
            }
        } else {
            if (browserImageLatestVersion.equals(v2)) {
                return 1;
            } else if (browserImageLatestVersion.equals(v1)) {
                return -1;
            }
        }
        return 0;
    }

    public boolean isNumericVersion(String version) {
        return version.matches("-?\\d+(\\.\\d+(\\.\\d+)?)?");
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

        Map<String, List<String>> browsers = this.getBrowsers(false);

        return format(browserImageFormat, browser,
                getVersionFromList(browsers.get(browser), version));
    }

    public Map<String, List<String>> getBrowsers(boolean cached)
            throws IOException {
        if (!etInternetDisabled && !cached) {
            this.cachedAvailableBrowsers = new TreeMap<>();

            List<DockerHubNameSpaceImage> imagesList = listImages();

            // [chrome, firefox, utils-get_browsers_version, utils-x11-base]
            log.trace("{} browser image list: {}", browserImageNamespace,
                    imagesList);

            for (DockerHubNameSpaceImage currentBrowserImage : imagesList) {
                String browser = currentBrowserImage.getName();
                if (!browser.toLowerCase()
                        .startsWith(browserImageSkipPrefix.toLowerCase())) {
                    List<DockerHubTag> tagList = listTags(
                            browserImageNamespace + "/" + browser);
                    log.trace("{} browser tag list: {}", browser, tagList);

                    for (DockerHubTag dockerHubTag : tagList) {
                        String tagName = dockerHubTag.getName();
                        String version = tagName;

                        if (browser.equalsIgnoreCase("opera")
                                && version.equalsIgnoreCase("12.16")) {
                            continue;
                        }

                        if (this.cachedAvailableBrowsers.containsKey(browser)) {
                            List<String> list = this.cachedAvailableBrowsers
                                    .get(browser);
                            list.add(version);

                            list = list.stream().sorted(this::compareVersions)
                                    .collect(toList());
                            this.cachedAvailableBrowsers.put(browser, list);
                        } else {
                            List<String> entry = new ArrayList<>();
                            entry.add(version);

                            this.cachedAvailableBrowsers.put(browser, entry);
                        }
                    }
                }
            }
        } else {
            // If there is not internet connection
            log.info("Internet is disabled, getting default images list");
            // If is empty, set manually
            if (this.cachedAvailableBrowsers.isEmpty()) {
                this.cachedAvailableBrowsers = this.getDefaultBrowsers();
            } // Else use cached
        }
        return this.cachedAvailableBrowsers;
    }

    public String getVersionFromImage(String image) {
        String version = image.split(":")[1];
        return version != null ? version : image;
    }

    public Map<String, List<String>> getDefaultBrowsers() {
        Map<String, List<String>> browsers = new TreeMap<>();
        List<String> chromeTags = new ArrayList<>();
        chromeTags.add("latest");
        chromeTags.add("69");
        chromeTags.add("68");
        chromeTags.add("67");
        chromeTags.add("66.0");
        chromeTags.add("65.0");
        chromeTags.add("64.0");
        chromeTags.add("63.0");
        chromeTags.add("62.0");
        chromeTags.add("61.0");
        chromeTags.add("60.0");
        chromeTags.add("unstable");
        chromeTags.add("beta");

        browsers.put("chrome", chromeTags);

        List<String> firefox = new ArrayList<>();
        firefox.add("latest");
        firefox.add("62");
        firefox.add("61");
        firefox.add("60.0");
        firefox.add("59.0");
        firefox.add("58.0");
        firefox.add("57.0");
        firefox.add("56.0");
        firefox.add("nightly");
        firefox.add("beta");

        browsers.put("firefox", firefox);

        return browsers;
    }
}
