package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.EusApplicationContextProvider;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.platform.manager.PlatformManager;
import io.elastest.eus.services.model.WebRTCQoEMeter;
import io.elastest.eus.session.SessionManager;

@Service
@DependsOn({ "eusContext" })
public class QoEService {
    final Logger log = getLogger(lookup().lookupClass());
    EusContextProperties contextProperties;
    EusFilesService eusFilesService;

    Map<String, WebRTCQoEMeter> webRTCQoEMeterMap = new HashMap<>();

    static boolean alreadyDestroyed = false;

    @Autowired
    public QoEService(EusFilesService eusFilesService) {
        super();
        this.eusFilesService = eusFilesService;
    }

    @PostConstruct
    public void init() {
        contextProperties = EusApplicationContextProvider
                .getContextPropertiesObject();
    }

    public void stopAndDestroy(SessionManager sessionManager) {
        if (!alreadyDestroyed && sessionManager != null
                && webRTCQoEMeterMap != null) {
            for (HashMap.Entry<String, WebRTCQoEMeter> webRTCQoEMeter : webRTCQoEMeterMap
                    .entrySet()) {
                try {
                    stopService(sessionManager, webRTCQoEMeter.getKey());
                } catch (Exception e) {
                    log.error("Error on stop QoEService {}",
                            webRTCQoEMeter.getKey());
                }
            }
            alreadyDestroyed = true;
        }
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
                .buildAndRunWebRTCQoEMeterService(sessionManager, execData,
                        labels);

        log.debug("WebRTC QoE Meter service started! Id: {}",
                webRTCQoEMeter.getIdentifier());
        addOrUpdateMap(webRTCQoEMeter);
        sessionManager.addEusServiceModelToList(webRTCQoEMeter);

        return webRTCQoEMeter.getIdentifier();
    }

    public void stopService(SessionManager sessionManager, String identifier)
            throws Exception {
        log.debug("Stopping WebRtcQoE service with id {}", identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();
        String serviceName = getRealServiceName(sessionManager, identifier);

        int killTimeoutInSeconds = 10;
        if (identifier != null
                && platformManager.existServiceWithName(serviceName)) {
            platformManager.removeServiceWithTimeout(serviceName,
                    killTimeoutInSeconds);
            removeWebRTCQoEMeter(identifier);
        }
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

    public void uploadVideos(SessionManager sessionManager, String identifier,
            String presenterFilePathInEus, String viewerFilePathInEus,
            String completePresenterPath, String completeViewerPath)
            throws Exception {
        log.debug("Uploading QoE Video files to service with id {}",
                identifier);
        String serviceName = getRealServiceName(sessionManager, identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();

        if (sessionManager.isAWSSession()) {
            // Upload presenter
            platformManager.uploadFileToSubserviceFromEus(serviceName,
                    identifier, presenterFilePathInEus, completePresenterPath);

            // Upload viewer
            platformManager.uploadFileToSubserviceFromEus(serviceName,
                    identifier, viewerFilePathInEus, completeViewerPath);
        } else {
            // Upload presenter
            platformManager.uploadFileFromEus(serviceName,
                    presenterFilePathInEus, completePresenterPath);

            // Upload viewer
            platformManager.uploadFileFromEus(serviceName, viewerFilePathInEus,
                    completeViewerPath);
        }

    }

    public void uploadVideos(SessionManager sessionManager, String identifier,
            String presenterFilePathInEus, String viewerFilePathInEus)
            throws Exception {

        String completePresenterPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PATH
                + "/"
                + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_ORIGINAL_VIDEO_NAME;
        String completeViewerPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PATH
                + "/"
                + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_RECEIVED_VIDEO_NAME;
        uploadVideos(sessionManager, identifier, presenterFilePathInEus,
                viewerFilePathInEus, completePresenterPath, completeViewerPath);
    }

    // Step 2
    public void downloadVideosFromBrowserAndUploadToQoE(
            SessionManager webRTCQoESessionManager,
            SessionManager presenterSessionManager,
            SessionManager viewerSessionManager, String identifier,
            String presenterCompleteFilePath, String viewerCompleteFilePath)
            throws Exception {
        final String eusDownloadFolder = eusFilesService
                .getSessionFilesFolderBySessionManager(webRTCQoESessionManager);

        /* ************** Presenter ************** */
        log.debug(
                "Downloading QoE Presenter Video file from session {} to send to service with id {}",
                viewerSessionManager.getSessionId(), identifier);

        final PlatformManager viewerPlatformManager = viewerSessionManager
                .getPlatformManager();
        String originalPresenterFileName = viewerPlatformManager
                .getFileNameFromCompleteFilePath(presenterCompleteFilePath);
        String newPresenterFileName = viewerSessionManager.getIdForFiles() + "_"
                + originalPresenterFileName;

        String presenterPathWithoutFile = viewerPlatformManager
                .getPathWithoutFileNameFromCompleteFilePath(
                        presenterCompleteFilePath);
        if (viewerSessionManager.isAWSSession()) {
            viewerPlatformManager.downloadFileOrFilesFromSubServiceToEus(
                    viewerSessionManager.getAwsInstanceId(),
                    viewerSessionManager.getVncContainerName(),
                    presenterPathWithoutFile, eusDownloadFolder,
                    originalPresenterFileName, newPresenterFileName, false);

        } else {
            viewerPlatformManager.downloadFileOrFilesFromServiceToEus(
                    viewerSessionManager.getVncContainerName(),
                    presenterPathWithoutFile, eusDownloadFolder,
                    originalPresenterFileName, false);

        }

        /* **************** Viewer **************** */
        log.debug(
                "Downloading QoE Viewer Video file from session {} to send to service with id {}",
                presenterSessionManager.getSessionId(), identifier);

        final PlatformManager presenterPlatformManager = presenterSessionManager
                .getPlatformManager();
        String originalViewerFileName = presenterPlatformManager
                .getFileNameFromCompleteFilePath(viewerCompleteFilePath);
        String newViewerFileName = presenterSessionManager.getIdForFiles() + "_"
                + originalViewerFileName;

        String viewerPathWithoutFile = presenterPlatformManager
                .getPathWithoutFileNameFromCompleteFilePath(
                        viewerCompleteFilePath);
        if (presenterSessionManager.isAWSSession()) {
            viewerPlatformManager.downloadFileOrFilesFromSubServiceToEus(
                    presenterSessionManager.getAwsInstanceId(),
                    presenterSessionManager.getVncContainerName(),
                    viewerPathWithoutFile, eusDownloadFolder,
                    originalViewerFileName, newViewerFileName, false);
        } else {
            presenterPlatformManager.downloadFileOrFilesFromServiceToEus(
                    presenterSessionManager.getVncContainerName(),
                    viewerPathWithoutFile, eusDownloadFolder,
                    originalViewerFileName, false);
        }

        /* **************** UPLOAD **************** */
        uploadVideos(webRTCQoESessionManager, identifier,
                eusDownloadFolder + newPresenterFileName,
                eusDownloadFolder + newViewerFileName);
    }

    public void calculateQoEMetrics(SessionManager sessionManager,
            String identifier) throws Exception {
        log.debug(
                "Calculating QoE metrics in service with ID {} . This process could take a long time.",
                identifier);
        String serviceName = getRealServiceName(sessionManager, identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();

        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);
        try {
            String command = "cd "
                    + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                    + "; " + "./"
                    + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPT_CALCULATE_FILENAME
                    + " >> /calculate.log";

            String result = platformManager.execCommandInSubService(serviceName,
                    identifier, true, command);
            log.info("CSV generated for service with id {}. Response: {}",
                    identifier, result);
        } catch (Exception e) {
            log.error("Error on generate QoE CSV for instance {}: {}",
                    identifier, e.getMessage());
            webRTCQoEMeter.setErrorOnCsvGeneration(true);
            throw e;
        }

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
    public boolean isCsvAlreadyGenerated(String identifier) throws Exception {
        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);
        if (webRTCQoEMeter != null) {
            if (webRTCQoEMeter.isErrorOnCsvGeneration()) {
                throw new Exception("there was an error generating the CSV");
            }
            return webRTCQoEMeter.isCsvGenerated();
        }
        return false;
    }

    // Step 5
    public List<byte[]> getQoEMetricsCSV(SessionManager sessionManager,
            String identifier) throws Exception {
        log.debug("Getting QoE Metrics CSV files for session {}",
                sessionManager.getSessionId());

        String serviceName = getRealServiceName(sessionManager, identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();

        List<byte[]> csvFiles = new ArrayList<byte[]>();
        List<String> csvFileNames = platformManager
                .getSubserviceFolderFilesList(serviceName, identifier,
                        contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH,
                        ".csv");

        if (csvFileNames != null) {
            for (String csvName : csvFileNames) {
                if (csvName != null && !"".equals(csvName)) {
                    String currentCsvPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                            + "/" + csvName;

                    InputStream currentCsv = null;
                    if (sessionManager.isAWSSession()) {
                        currentCsv = platformManager.getFileFromSubService(
                                serviceName, identifier, currentCsvPath, false);
                    } else {
                        currentCsv = platformManager.getFileFromService(
                                serviceName, currentCsvPath, false);
                    }

                    if (currentCsv != null) {
                        csvFiles.add(IOUtils.toByteArray(currentCsv));
                    }
                }
            }
        }

        return csvFiles;
    }

    public List<Double> getQoEMetricsMetric(SessionManager sessionManager,
            String identifier) throws Exception {
        List<Double> metrics = new ArrayList<>();
        List<byte[]> csvs = getQoEMetricsCSV(sessionManager, identifier);

        if (csvs != null) {
            for (byte[] csv : csvs) {
                Double average = 0.0;
                int total = 0;

                InputStream is = null;
                BufferedReader bfReader = null;

                is = new ByteArrayInputStream(csv);
                bfReader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = bfReader.readLine()) != null) {
                    try {
                        Double lineAsNum = Double.valueOf(line);
                        average = average + lineAsNum;
                        total++;
                    } catch (Exception e) {
                    }
                }

                average = average / total;
                if (is != null) {
                    is.close();
                }
                metrics.add(average);

            }
        }

        return metrics;
    }

    private String getRealServiceName(SessionManager sessionManager,
            String identifier) throws Exception {
        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);

        if (webRTCQoEMeter != null) {
            // Default docker/k8s
            String serviceName = identifier;

            // If AWS session
            if (sessionManager.isAWSSession()
                    && webRTCQoEMeter.getAwsInstanceId() != null) {
                serviceName = webRTCQoEMeter.getAwsInstanceId();
            }
            return serviceName;
        } else {
            throw new Exception(
                    "Error on upload videos to QoE service: Identifier "
                            + identifier + " not found");
        }
    }
}
