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

import static io.elastest.eus.docker.DockerContainer.dockerBuilder;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.matches;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static okhttp3.MediaType.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;

import io.elastest.eus.docker.DockerException;
import io.elastest.eus.external.DockerComposeConfig;
import io.elastest.eus.external.DockerComposeList;
import io.elastest.eus.external.DockerComposeProject;
import io.elastest.eus.external.DockerComposeUiApi;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Service implementation for Docker Compose.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class DockerComposeService {

    private final Logger log = LoggerFactory
            .getLogger(DockerComposeService.class);

    @Value("${docker.compose.ui.exposedport}")
    private int dockerComposeUiPort;

    @Value("${docker.compose.ui.image}")
    private String dockerComposeUiImageId;

    @Value("${docker.compose.ui.prefix}")
    private String dockerComposeUiPrefix;

    @Value("${docker.compose.ui.timeout}")
    private int dockerComposeTimeout;

    @Value("${docker.default.socket}")
    private String dockerDefaultSocket;

    private String dockerComposeUiContainerName;
    private DockerComposeUiApi dockerComposeUi;

    private DockerService dockerService;

    public DockerComposeService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @PostConstruct
    public void setup() throws IOException, InterruptedException {
        dockerComposeUiContainerName = dockerService
                .generateContainerName(dockerComposeUiPrefix);

        int dockerComposeBindPort = dockerService.findRandomOpenPort();
        Binding bindNoVncPort = Ports.Binding.bindPort(dockerComposeBindPort);
        ExposedPort exposedNoVncPort = ExposedPort.tcp(dockerComposeUiPort);

        String dockerComposeUiUrl = "http://"
                + dockerService.getDockerServerIp() + ":"
                + dockerComposeBindPort;

        log.debug("Starting docker-compose-ui container: {}",
                dockerComposeUiContainerName);

        List<PortBinding> portBindings = asList(
                new PortBinding(bindNoVncPort, exposedNoVncPort));
        Volume volume = new Volume(dockerDefaultSocket);
        List<Volume> volumes = asList(volume);
        List<Bind> binds = asList(new Bind(dockerDefaultSocket, volume));

        dockerService
                .startAndWaitContainer(dockerBuilder(dockerComposeUiImageId,
                        dockerComposeUiContainerName).portBindings(portBindings)
                                .volumes(volumes).binds(binds).build());

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(dockerComposeTimeout, SECONDS)
                .connectTimeout(dockerComposeTimeout, SECONDS).build();
        Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(dockerComposeUiUrl).build();
        dockerComposeUi = retrofit.create(DockerComposeUiApi.class);

        log.debug("docker-compose-ui up and running on URL: {}",
                dockerComposeUiUrl);
    }

    @PreDestroy
    public void teardown() {
        log.debug("Stopping docker-compose-ui container: {}",
                dockerComposeUiContainerName);
        dockerService.stopAndRemoveContainer(dockerComposeUiContainerName);
    }

    public DockerComposeProject createAndStartDockerComposeProject(
            String projectName, String dockerComposeYml) throws IOException {
        return new DockerComposeProject(projectName, dockerComposeYml, this);
    }

    public boolean createProject(String projectName, String dockerComposeYml)
            throws IOException {
        assert matches("^[a-zA-Z0-9]*$", projectName);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", projectName);
        jsonObject.put("yml", dockerComposeYml.replaceAll("'", "\""));
        RequestBody data = RequestBody.create(parse(APPLICATION_JSON),
                jsonObject.toString());

        log.trace("Creating Docker Compose with data: {}", jsonObject);
        Response<ResponseBody> response = dockerComposeUi.createProject(data)
                .execute();

        log.trace("Create project response code {}", response.code());
        if (!response.isSuccessful()) {
            throw new DockerException(response.errorBody().string());
        }

        return true;
    }

    public boolean startProject(String projectName) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", projectName);

        RequestBody data = RequestBody.create(parse(APPLICATION_JSON),
                jsonObject.toString());

        log.trace("Starting Docker Compose project with data: {}", jsonObject);
        Response<ResponseBody> response = dockerComposeUi.dockerComposeUp(data)
                .execute();

        log.trace("Start project response code {}", response.code());
        if (!response.isSuccessful()) {
            throw new DockerException(response.errorBody().string());
        }
        return true;
    }

    public boolean stopProject(String projectName) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", projectName);
        RequestBody data = RequestBody.create(parse(APPLICATION_JSON),
                jsonObject.toString());

        log.trace("Stopping Docker Compose project with data: {}", jsonObject);
        Response<ResponseBody> response = dockerComposeUi
                .dockerComposeDown(data).execute();

        log.trace("Stop project response code {}", response.code());
        if (!response.isSuccessful()) {
            throw new DockerException(response.errorBody().string());
        }
        return true;
    }

    public List<DockerComposeProject> listProjects() throws IOException {
        log.debug("List Docker Compose projects");
        List<DockerComposeProject> projects = new ArrayList<>();

        Response<DockerComposeList> response = dockerComposeUi.listProjects()
                .execute();
        log.debug("List projects response code {}", response.code());

        if (response.isSuccessful()) {
            DockerComposeList body = response.body();
            log.debug("Success: {}", body);
            List<Object> active = body.getActive();

            for (Object o : active) {
                if (o.getClass() == String.class) {
                    projects.add(new DockerComposeProject(o.toString(), this));
                }
            }
        }
        return projects;
    }

    public String getYaml(String projectName) throws IOException {
        log.debug("Get YAML of project {}", projectName);

        Response<DockerComposeConfig> response = dockerComposeUi
                .getDockerComposeYml(projectName).execute();
        log.debug("Get YAML response code {}", response.code());

        if (response.isSuccessful()) {
            return response.body().getYml();
        }
        throw new DockerException(response.errorBody().string());
    }

}
