package io.elastest.eus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EusContextProperties {

    @Value("${api.context.path}")
    public String API_CONTEXT_PATH;
    @Value("${eus.container.prefix}")
    public String EUS_CONTAINER_PREFIX;

    @Value("${hub.exposedport}")
    public int HUB_EXPOSED_PORT;
    @Value("${hub.vnc.exposedport}")
    public int HUB_VNC_EXPOSED_PORT;
    @Value("${hub.novnc.exposedport}")
    public int NO_VNC_EXPOSED_PORT;
    @Value("${hub.container.sufix}")
    public String HUB_CONTAINER_SUFIX;
    @Value("${novnc.html}")
    public String VNC_HTML;
    @Value("${hub.vnc.password}")
    public String HUB_VNC_PASSWORD;

    @Value("${et.host.env}")
    public String ET_HOST_ENV;
    @Value("${et.host.type.env}")
    public String ET_HOST_ENV_TYPE;

    @Value("${ws.dateformat}")
    public String WS_DATE_FORMAT;

    @Value("${et.enable.cloud.mode}")
    public boolean ENABLE_CLOUD_MODE;

    // Defined as String instead of integer for testing purposes (inject with
    // @TestPropertySource)
    @Value("${hub.timeout}")
    public String HUB_TIMEOUT;

    @Value("${browser.screen.resolution}")
    public String BROWSER_SCREEN_RESOLUTION;
    @Value("${browser.timezone}")
    public String BROWSER_TIMEZONE;
    @Value("${webdriver.session.message}")
    public String WEBDRIVER_SESSION_MESSAGE;
    @Value("${webdriver.crossbrowser.session.message}")
    public String WEBDRIVER_CROSSBROWSER_SESSION_MESSAGE;
    @Value("${webdriver.navigation.get.message}")
    public String WEBDRIVER_NAVIGATION_GET_MESSAGE;
    @Value("${webdriver.execute.script.message}")
    public String WEBDRIVER_EXECUTE_SCRIPT_MESSAGE;
    @Value("${webdriver.execute.sync.script.message}")
    public String WEBDRIVER_EXECUTE_SYNC_SCRIPT_MESSAGE;
    @Value("${webdriver.execute.async.script.message}")
    public String WEBDRIVER_EXECUTE_ASYNC_SCRIPT_MESSAGE;

    @Value("${et.intercept.script.prefix}")
    public String ET_INTERCEPT_SCRIPT_PREFIX;
    @Value("${et.intercept.script.escaped.prefix}")
    public String ET_INTERCEPT_SCRIPT_ESCAPED_PREFIX;

    @Value("${create.session.timeout.sec}")
    public int CREATE_SESSION_TIMEOUT_SEC;
    @Value("${create.session.retries}")
    public int CREATE_SESSION_RETRIES;
    @Value("${et.config.web.rtc.stats}")
    public String ET_CONFIG_WEB_RTC_STATS;
    @Value("${et.mon.interval}")
    public String ET_MON_INTERVAL;
    @Value("${et.browser.component.prefix}")
    public String ET_BROWSER_COMPONENT_PREFIX;

    /* *** ET container labels *** */
    @Value("${et.type.label}")
    public String ET_TYPE_LABEL;
    @Value("${et.tjob.id.label}")
    public String ET_TJOB_ID_LABEL;
    @Value("${et.tjob.exec.id.label}")
    public String ET_TJOB_EXEC_ID_LABEL;

    @Value("${et.tjob.sut.service.name.label}")
    public String ET_TJOB_SUT_SERVICE_NAME_LABEL;
    @Value("${et.tjob.tss.id.label}")
    public String ET_TJOB_TSS_ID_LABEL;
    @Value("${et.tjob.tss.type.label}")
    public String ET_TJOB_TSS_TYPE_LABEL;
    @Value("${et.type.test.label.value}")
    public String ET_TYPE_TEST_LABEL_VALUE;
    @Value("${et.type.sut.label.value}")
    public String ET_TYPE_SUT_LABEL_VALUE;
    @Value("${et.type.tss.label.value}")
    public String ET_TYPE_TSS_LABEL_VALUE;
    @Value("${et.type.core.label.value}")
    public String ET_TYPE_CORE_LABEL_VALUE;
    @Value("${et.type.te.label.value}")
    public String ET_TYPE_TE_LABEL_VALUE;
    @Value("${et.type.monitoring.label.value}")
    public String ET_TYPE_MONITORING_LABEL_VALUE;
    @Value("${et.type.tool.label.value}")
    public String ET_TYPE_TOOL_LABEL_VALUE;

    @Value("${browser.shm.size}")
    public long SHM_SIZE;

    @Value("${eus.service.browsersync.prefix}")
    public String EUS_SERVICE_BROWSERSYNC_PREFIX;

    @Value("${eus.service.browsersync.image.name}")
    public String EUS_SERVICE_BROWSERSYNC_IMAGE_NAME;

    @Value("${eus.service.browsersync.gui.port}")
    public String EUS_SERVICE_BROWSERSYNC_GUI_PORT;

    @Value("${eus.service.browsersync.app.port}")
    public String EUS_SERVICE_BROWSERSYNC_APP_PORT;

    @Value("${host.shared.files.relative.folder}")
    public String HOST_SHARED_FILES_RELATIVE_FOLDER;

    @Value("${container.recording.folder}")
    public String CONTAINER_RECORDING_FOLDER;

    @Value("${container.main.folder}")
    public String CONTAINER_MAIN_FOLDER;

    @Value("${container.shared.files.folder}")
    public String CONTAINER_SHARED_FILES_FOLDER;

    @Value("${use.torm}")
    public boolean USE_TORM;
    @Value("${docker.network}")
    public String DOCKER_NETWORK;

    /* ************ WebRTC QoE Meter ************ */

    @Value("${eus.service.webrtc.qoe.meter.prefix}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_PREFIX;

    @Value("${eus.service.webrtc.qoe.meter.image.name}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_NAME;

    @Value("${eus.service.webrtc.qoe.meter.image.command}")
    public String EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_COMMAND;

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