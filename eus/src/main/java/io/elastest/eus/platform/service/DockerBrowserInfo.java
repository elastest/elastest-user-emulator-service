package io.elastest.eus.platform.service;

import io.elastest.epm.client.model.DockerServiceStatus;

public class DockerBrowserInfo extends DockerServiceStatus {

    protected int hubPort;
    protected int noVncBindedPort;
    private String hubContainerName;
    private String vncContainerName;
    protected String hubIp;
    protected String vncUrl;
    protected String hostSharedFilesFolderPath;
    protected String browserPod;
    
    public String getHubContainerName() {
        return hubContainerName;
    }

    public void setHubContainerName(String hubContainerName) {
        this.hubContainerName = hubContainerName;
    }

    public int getHubPort() {
        return hubPort;
    }

    public void setHubPort(int hubPort) {
        this.hubPort = hubPort;
    }

    public int getNoVncBindedPort() {
        return noVncBindedPort;
    }

    public void setNoVncBindedPort(int noVncBindedPort) {
        this.noVncBindedPort = noVncBindedPort;
    }

    public String getHubIp() {
        return hubIp;
    }

    public void setHubIp(String hubIp) {
        this.hubIp = hubIp;
    }
    
    public String getVncUrl() {
        return vncUrl;
    }

    public void setVncUrl(String vncUrl) {
        this.vncUrl = vncUrl;
    }

    public String getVncContainerName() {
        return vncContainerName;
    }

    public void setVncContainerName(String vncContainerName) {
        this.vncContainerName = vncContainerName;
    }
    
    public String getHostSharedFilesFolderPath() {
        return hostSharedFilesFolderPath;
    }

    public void setHostSharedFilesFolderPath(String hostSharedFilesFolderPath) {
        this.hostSharedFilesFolderPath = hostSharedFilesFolderPath;
    }

    public String getBrowserPod() {
        return browserPod;
    }

    public void setBrowserPod(String browserPod) {
        this.browserPod = browserPod;
    }

}
