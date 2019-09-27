package io.elastest.eus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EusContextProperties {
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

    /* *** ET container labels *** */
    @Value("${et.type.label}")
    public String etTypeLabel;
    @Value("${et.tjob.id.label}")
    public String etTJobIdLabel;
    @Value("${et.tjob.exec.id.label}")
    public String etTJobExecIdLabel;

    @Value("${et.tjob.sut.service.name.label}")
    public String etTJobSutServiceNameLabel;
    @Value("${et.tjob.tss.id.label}")
    public String etTJobTSSIdLabel;
    @Value("${et.tjob.tss.type.label}")
    public String etTJobTssTypeLabel;

    @Value("${et.type.test.label.value}")
    public String etTypeTestLabelValue;
    @Value("${et.type.sut.label.value}")
    public String etTypeSutLabelValue;
    @Value("${et.type.tss.label.value}")
    public String etTypeTSSLabelValue;
    @Value("${et.type.core.label.value}")
    public String etTypeCoreLabelValue;
    @Value("${et.type.te.label.value}")
    public String etTypeTELabelValue;
    @Value("${et.type.monitoring.label.value}")
    public String etTypeMonitoringLabelValue;
    @Value("${et.type.tool.label.value}")
    public String etTypeToolLabelValue;

    /* ************ WebRTC QoE Meter ************ */

    @Value("${eus.service.webrtc.qoe.meter.prefix}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_PREFIX;

    @Value("${eus.service.webrtc.qoe.meter.image.name}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_NAME;

    @Value("${eus.service.webrtc.qoe.meter.path}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_PATH;

    @Value("${eus.service.webrtc.qoe.meter.scripts.path}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH;

    @Value("${eus.service.webrtc.qoe.meter.script.calculate.filename}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_SCRIPT_CALCULATE_FILENAME;

    @Value("${eus.service.webrtc.qoe.meter.original.video.name}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_ORIGINAL_VIDEO_NAME;

    @Value("${eus.service.webrtc.qoe.meter.received.video.name}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_RECEIVED_VIDEO_NAME;

}
