package io.elastest.eus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContextProperties {
    @Value("${hub.exposedport}")
    public int hubExposedPort;
    @Value("${hub.vnc.exposedport}")
    public int hubVncExposedPort;
    @Value("${hub.novnc.exposedport}")
    public int noVncExposedPort;
    @Value("${browser.shm.size}")
    public long shmSize;

    @Value("${eus.container.prefix}")
    public String eusContainerPrefix;

    @Value("${eus.service.browsersync.prefix}")
    public String eusServiceBrowsersyncPrefix;

    @Value("${eus.service.browsersync.image.name}")
    public String eusServiceBrowsersyncImageName;

    @Value("${eus.service.browsersync.gui.port}")
    public String eusServiceBrowsersyncGUIPort;

    @Value("${eus.service.browsersync.app.port}")
    public String eusServiceBrowsersyncAppPort;

    @Value("${host.shared.files.relative.folder}")
    public String hostSharedFilesRelativeFolder;

    @Value("${container.recording.folder}")
    public String containerRecordingFolder;

    @Value("${container.shared.files.folder}")
    public String containerSharedFilesFolder;

    @Value("${browser.screen.resolution}")
    public String browserScreenResolution;

    @Value("${use.torm}")
    public boolean useTorm;
    @Value("${docker.network}")
    public String dockerNetwork;

}
