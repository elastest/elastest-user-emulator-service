package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.EusApplicationContextProvider;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.services.model.WebRTCQoEMeter;
import io.elastest.eus.session.SessionManager;

@Service
@DependsOn({ "eusContext" })
public class QoEService {
    final Logger log = getLogger(lookup().lookupClass());
    EusContextProperties contextProperties;

    Map<String, WebRTCQoEMeter> webRTCQoEMeterMap = new HashMap<>();

    @PostConstruct
    public void init() {
        contextProperties = EusApplicationContextProvider
                .getContextPropertiesObject();
    }

    /* ************************************ */
    /* **** Manage QoE service methods **** */
    /* ************************************ */

    // Step 1
    public String startService(SessionManager sessionManager) throws Exception {
        Map<String, String> labels = new HashMap<>();
        labels.put(contextProperties.ET_TYPE_LABEL,
                contextProperties.ET_TYPE_TSS_LABEL_VALUE);
        labels.put(contextProperties.ET_TJOB_TSS_TYPE_LABEL, "aux");

        final ExecutionData execData = sessionManager
                .getElastestExecutionData();
        if (execData != null) {
            labels.put(contextProperties.ET_TJOB_EXEC_ID_LABEL,
                    execData.gettJobExecId().toString());
            labels.put(contextProperties.ET_TJOB_ID_LABEL,
                    execData.gettJobId().toString());
        }

        WebRTCQoEMeter webRTCQoEMeter = sessionManager.getPlatformManager()
                .buildAndRunWebRTCQoEMeterService(execData, labels);

        addOrUpdateMap(webRTCQoEMeter);

        return webRTCQoEMeter.getIdentifier();
    }

    public void addOrUpdateMap(WebRTCQoEMeter webRTCQoEMeter) {
        if (webRTCQoEMeterMap != null && webRTCQoEMeter != null
                && !"".equals(webRTCQoEMeter.getIdentifier())) {
            webRTCQoEMeterMap.put(webRTCQoEMeter.getIdentifier(),
                    webRTCQoEMeter);
        }
    }

    public WebRTCQoEMeter getWebRTCQoEMeter(String identifier) {
        if (webRTCQoEMeterMap != null
                && webRTCQoEMeterMap.containsKey(identifier)) {
            return webRTCQoEMeterMap.get(identifier);
        }
        return null;
    }

    public void removeWebRTCQoEMeter(String identifier) {
        if (webRTCQoEMeterMap != null
                && webRTCQoEMeterMap.containsKey(identifier)) {
            webRTCQoEMeterMap.remove(identifier);
        }
    }

    /* ************************************ */
    /* ***** WebRTC QoE meter methods ***** */
    /* ************************************ */

    public void uploadVideos(SessionManager sessionManager,
            String serviceNameOrId, InputStream presenterFile,
            InputStream viewerFile, String completePresenterPath,
            String completeViewerPath) throws Exception {
        log.debug(
                "Uploading QoE Video files of session {} to service with id {}",
                sessionManager.getSessionId(), serviceNameOrId);

        // Upload presenter
        sessionManager.getPlatformManager().uploadFile(serviceNameOrId,
                presenterFile, completePresenterPath);

        // Upload viewer
        sessionManager.getPlatformManager().uploadFile(serviceNameOrId,
                viewerFile, completeViewerPath);
    }

    public void uploadVideos(SessionManager sessionManager,
            String serviceNameOrId, InputStream presenterFile,
            InputStream viewerFile) throws Exception {

        String completePresenterPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PATH
                + "/"
                + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_ORIGINAL_VIDEO_NAME;
        String completeViewerPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PATH
                + "/"
                + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_RECEIVED_VIDEO_NAME;
        uploadVideos(sessionManager, serviceNameOrId, presenterFile, viewerFile,
                completePresenterPath, completeViewerPath);
    }

    // Step 2
    public void downloadVideosFromBrowserAndUploadToQoE(
            SessionManager sessionManager, String serviceNameOrId,
            String presenterCompleteFilePath, String viewerCompleteFilePath)
            throws Exception {
        log.debug(
                "Downloading QoE Video files from session {} to send to service with id {}",
                sessionManager.getSessionId(), serviceNameOrId);

        InputStream presenterVideo = sessionManager.getPlatformManager()
                .getFileFromBrowser(sessionManager, presenterCompleteFilePath,
                        false);

        InputStream viewerVideo = sessionManager.getPlatformManager()
                .getFileFromBrowser(sessionManager, viewerCompleteFilePath,
                        false);

        uploadVideos(sessionManager, serviceNameOrId, presenterVideo,
                viewerVideo);
    }

    public void calculateQoEMetrics(SessionManager sessionManager,
            String serviceNameOrId) throws Exception {
        log.debug(
                "Calculating QoE metrics for session {} in service with ID {} . This process could take a long time.",
                sessionManager.getSessionId(), serviceNameOrId);
        sessionManager.getPlatformManager().execCommand(serviceNameOrId, true,
                "sh", "-c",
                "'" + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                        + "/"
                        + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPT_CALCULATE_FILENAME
                        + "'");

        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(serviceNameOrId);
        if (webRTCQoEMeter != null) {
            webRTCQoEMeter.setCsvGenerated(true);
            addOrUpdateMap(webRTCQoEMeter);
        }

    }

    // Step 3
    @Async
    public void calculateQoEMetricsAsync(SessionManager sessionManager,
            String serviceNameOrId) throws Exception {
        calculateQoEMetrics(sessionManager, serviceNameOrId);
    }

    // Step 4
    public boolean isCsvAlreadyGenerated(String identifier) {
        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);
        if (webRTCQoEMeter != null) {
            return webRTCQoEMeter.isCsvGenerated();
        }
        return false;
    }

    // Step 5
    public List<InputStream> getQoEMetricsCSV(SessionManager sessionManager,
            String serviceNameOrId) throws Exception {
        log.debug("Getting QoE Metrics CSV files for session {}",
                sessionManager.getSessionId());
        List<InputStream> csvFiles = new ArrayList<InputStream>();
        List<String> csvFileNames = sessionManager.getPlatformManager()
                .getFolderFilesList(serviceNameOrId,
                        contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH,
                        ".csv");

        if (csvFileNames != null) {
            for (String csvName : csvFileNames) {
                if (csvName != null && !"".equals(csvName)) {
                    String currentCsvPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                            + "/" + csvName;
                    InputStream currentCsv = sessionManager.getPlatformManager()
                            .getFileFromBrowser(sessionManager, currentCsvPath,
                                    false);
                    csvFiles.add(currentCsv);
                }
            }
        }

        return csvFiles;
    }

}
