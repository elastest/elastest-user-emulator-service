package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        contextProperties = EusApplicationContextProvider.getContextPropertiesObject();
    }

    public void stopAndDestroy(SessionManager sessionManager) {
        if (!alreadyDestroyed && sessionManager != null && webRTCQoEMeterMap != null) {
            for (HashMap.Entry<String, WebRTCQoEMeter> webRTCQoEMeter : webRTCQoEMeterMap
                    .entrySet()) {
                try {
                    stopService(sessionManager, webRTCQoEMeter.getKey());
                } catch (Exception e) {
                    log.error("Error on stop QoEService {}", webRTCQoEMeter.getKey());
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
        labels.put(contextProperties.ET_TYPE_LABEL, contextProperties.ET_TYPE_TSS_LABEL_VALUE);
        labels.put(contextProperties.ET_TJOB_TSS_TYPE_LABEL, "aux");

        final ExecutionData execData = sessionManager.getElastestExecutionData();
        if (execData != null) {
            labels.put(contextProperties.ET_TJOB_EXEC_ID_LABEL,
                    execData.gettJobExecId().toString());
            labels.put(contextProperties.ET_TJOB_ID_LABEL, execData.gettJobId().toString());
        }

        WebRTCQoEMeter webRTCQoEMeter = sessionManager.getPlatformManager()
                .buildAndRunWebRTCQoEMeterService(sessionManager, execData, labels);

        log.debug("WebRTC QoE Meter service started! Id: {}", webRTCQoEMeter.getIdentifier());
        addOrUpdateMap(webRTCQoEMeter);
        sessionManager.addEusServiceModelToList(webRTCQoEMeter);

        return webRTCQoEMeter.getIdentifier();
    }

    public void stopService(SessionManager sessionManager, String identifier) throws Exception {
        log.debug("Stopping WebRtcQoE service with id {}", identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();
        String serviceName = getRealServiceName(sessionManager, identifier);

        int killTimeoutInSeconds = 10;
        if (identifier != null && platformManager.existServiceWithName(serviceName)) {
            platformManager.removeServiceWithTimeout(serviceName, killTimeoutInSeconds);
            removeWebRTCQoEMeter(identifier);
        }
    }

    public void addOrUpdateMap(WebRTCQoEMeter webRTCQoEMeter) {
        if (webRTCQoEMeterMap != null && webRTCQoEMeter != null
                && !"".equals(webRTCQoEMeter.getIdentifier())) {
            webRTCQoEMeterMap.put(webRTCQoEMeter.getIdentifier(), webRTCQoEMeter);
        }
    }

    public WebRTCQoEMeter getWebRTCQoEMeter(String identifier) {
        if (webRTCQoEMeterMap != null && webRTCQoEMeterMap.containsKey(identifier)) {
            return webRTCQoEMeterMap.get(identifier);
        }
        return null;
    }

    public void removeWebRTCQoEMeter(String identifier) {
        if (webRTCQoEMeterMap != null && webRTCQoEMeterMap.containsKey(identifier)) {
            webRTCQoEMeterMap.remove(identifier);
        }
    }

    /* ************************************ */
    /* ***** WebRTC QoE meter methods ***** */
    /* ************************************ */

    public void uploadVideos(SessionManager sessionManager, String identifier,
            String originalFilePathInEus, String originalFileNameInEus,
            String receivedFilePathInEus, String receivedFileNameInEus,
            String targetCompleteOriginalVideoPath, String targetCompleteOriginalVideoName,
            String targetCompleteReceivedVideoPath, String targetCompleteReceivedVideoName)
            throws Exception {
        log.debug("Uploading QoE Video files to service with id {}", identifier);
        String serviceName = getRealServiceName(sessionManager, identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();

        if (sessionManager.isAWSSession()) {
            // Upload original video (from presenter)
            platformManager.uploadFileToSubserviceFromEus(sessionManager, serviceName, identifier,
                    originalFilePathInEus, originalFileNameInEus, targetCompleteOriginalVideoPath,
                    targetCompleteOriginalVideoName);

            // Upload received video (from viewer)
            platformManager.uploadFileToSubserviceFromEus(sessionManager, serviceName, identifier,
                    receivedFilePathInEus, receivedFileNameInEus, targetCompleteReceivedVideoPath,
                    targetCompleteReceivedVideoName);
        } else {
            // Upload original video (from presenter)
            platformManager.uploadFileFromEus(sessionManager, serviceName, originalFilePathInEus,
                    originalFileNameInEus, targetCompleteOriginalVideoPath,
                    targetCompleteOriginalVideoName);

            // Upload received video (from viewer)
            platformManager.uploadFileFromEus(sessionManager, serviceName, receivedFilePathInEus,
                    receivedFileNameInEus, targetCompleteReceivedVideoPath,
                    targetCompleteReceivedVideoName);
        }

    }

    public void uploadVideos(SessionManager sessionManager, String identifier,
            String presenterFilePathWithNameInEus, String viewerFilePathWithNameInEus)
            throws Exception {

        String presenterFilePathInEus = eusFilesService
                .getPathWithoutFileNameFromCompleteFilePath(presenterFilePathWithNameInEus);
        String presenterFileNameInEus = eusFilesService
                .getFileNameFromCompleteFilePath(presenterFilePathWithNameInEus);

        String viewerFilePathInEus = eusFilesService
                .getPathWithoutFileNameFromCompleteFilePath(viewerFilePathWithNameInEus);
        String viewerFileNameInEus = eusFilesService
                .getFileNameFromCompleteFilePath(viewerFilePathWithNameInEus);

        uploadVideos(sessionManager, identifier, presenterFilePathInEus, presenterFileNameInEus,
                viewerFilePathInEus, viewerFileNameInEus,
                contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PATH,
                contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_ORIGINAL_VIDEO_NAME,
                contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PATH,
                contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_RECEIVED_VIDEO_NAME);
    }

    // Step 2
    public void downloadVideosFromBrowserAndUploadToQoE(SessionManager webRTCQoESessionManager,
            SessionManager presenterSessionManager, SessionManager viewerSessionManager,
            String identifier, String presenterCompleteFilePath, String viewerCompleteFilePath)
            throws Exception {
        final String eusDownloadFolder = eusFilesService
                .getSessionFilesFolderBySessionManager(webRTCQoESessionManager);

        /* ************** Presenter ************** */
        log.debug(
                "Downloading QoE Presenter Video file from session {} to send to service with id {}",
                viewerSessionManager.getSessionId(), identifier);

        final PlatformManager viewerPlatformManager = viewerSessionManager.getPlatformManager();
        String originalPresenterFileName = eusFilesService
                .getFileNameFromCompleteFilePath(presenterCompleteFilePath);
        String newPresenterFileName = viewerSessionManager.getIdForFiles() + "_"
                + originalPresenterFileName;

        String presenterPathWithoutFile = eusFilesService
                .getPathWithoutFileNameFromCompleteFilePath(presenterCompleteFilePath);

        String viewerInstanceId = null;
        String viewerServiceName = null;
        if (viewerSessionManager.isAWSSession()) {
            viewerInstanceId = viewerSessionManager.getAwsInstanceId();
            viewerServiceName = viewerSessionManager.getVncContainerName();
        } else {
            viewerInstanceId = viewerSessionManager.getVncContainerName();
            viewerServiceName = viewerSessionManager.getVncContainerName();
        }
        viewerPlatformManager.downloadFileOrFilesFromSubServiceToEus(viewerInstanceId,
                viewerServiceName, presenterPathWithoutFile, eusDownloadFolder,
                originalPresenterFileName, newPresenterFileName, false);

        /* **************** Viewer **************** */
        log.debug("Downloading QoE Viewer Video file from session {} to send to service with id {}",
                presenterSessionManager.getSessionId(), identifier);

        String originalViewerFileName = eusFilesService
                .getFileNameFromCompleteFilePath(viewerCompleteFilePath);
        String newViewerFileName = presenterSessionManager.getIdForFiles() + "_"
                + originalViewerFileName;

        String viewerPathWithoutFile = eusFilesService
                .getPathWithoutFileNameFromCompleteFilePath(viewerCompleteFilePath);

        String presenterInstanceId = null;
        String presenterServiceName = null;

        if (presenterSessionManager.isAWSSession()) {
            presenterInstanceId = presenterSessionManager.getAwsInstanceId();
            presenterServiceName = presenterSessionManager.getVncContainerName();
        } else {
            presenterInstanceId = presenterSessionManager.getVncContainerName();
            presenterServiceName = presenterSessionManager.getVncContainerName();
        }

        viewerPlatformManager.downloadFileOrFilesFromSubServiceToEus(presenterInstanceId,
                presenterServiceName, viewerPathWithoutFile, eusDownloadFolder,
                originalViewerFileName, newViewerFileName, false);

        /* **************** UPLOAD **************** */
        uploadVideos(webRTCQoESessionManager, identifier, eusDownloadFolder + newPresenterFileName,
                eusDownloadFolder + newViewerFileName);
    }

    public void calculateQoEMetrics(SessionManager sessionManager, String identifier)
            throws Exception {
        log.debug(
                "Calculating QoE metrics in service with ID {} . This process could take a long time.",
                identifier);
        String serviceName = getRealServiceName(sessionManager, identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();

        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);
        try {

            String result = "";
            if (sessionManager.isAWSSession()) {
                String command = "cd " + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                        + "; " + "./"
                        + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPT_CALCULATE_FILENAME;

                result = platformManager.execCommandInSubService(serviceName, identifier, true,
                        command);
            } else {
                String command = "cd " + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                        + "; " + "./"
                        + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPT_CALCULATE_FILENAME;

                result = platformManager.execCommand(serviceName, command);
            }
            log.info("CSV generated for service with id {}. Response: {}", identifier, result);

            result = platformManager.execCommandInSubService(serviceName, identifier, true,
                    "ls " + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH);
            log.debug("ls command response for service with id {}. Response: {}", identifier,
                    result);

        } catch (Exception e) {
            log.error("Error on generate QoE CSV for instance {}: {}", identifier, e.getMessage());
            webRTCQoEMeter.setErrorOnCsvGeneration(true);
            throw e;
        }

        if (webRTCQoEMeter != null) {

            try {
                Map<String, byte[]> csvs = obtainQoEMetricsCSV(sessionManager, identifier);
                webRTCQoEMeter.setCsvs(csvs);
            } catch (Exception e) {
                log.error("Error on getting qoe csvs in {}: {}", identifier, e.getMessage());
            }

            webRTCQoEMeter.setCsvGenerated(true);
            addOrUpdateMap(webRTCQoEMeter);

            getQoEAverageMetrics(sessionManager, identifier, true);
        }

    }

    // Step 3
    @Async
    public void calculateQoEMetricsAsync(SessionManager sessionManager, String serviceNameOrId)
            throws Exception {
        calculateQoEMetrics(sessionManager, serviceNameOrId);
    }

    // Obtain csvs from docker/k8s/aws and save in memory
    private Map<String, byte[]> obtainQoEMetricsCSV(SessionManager sessionManager,
            String identifier) throws Exception {
        log.debug("Getting QoE Metrics CSV files for session {}", sessionManager.getSessionId());

        String serviceName = getRealServiceName(sessionManager, identifier);
        PlatformManager platformManager = sessionManager.getPlatformManager();

        Map<String, byte[]> csvFiles = new HashMap<String, byte[]>();
        List<String> csvFileNames = platformManager.getSubserviceFolderFilesList(serviceName,
                identifier, contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH, ".csv");

        log.debug("Obtained CSV files names for service {}: {}", serviceName, csvFileNames);

        if (csvFileNames != null) {
            for (String csvName : csvFileNames) {
                if (csvName != null && !"".equals(csvName)) {
                    String currentCsvPath = contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_SCRIPTS_PATH
                            + "/" + csvName;

                    InputStream currentCsv = null;
                    if (sessionManager.isAWSSession()) {
                        currentCsv = platformManager.getFileFromSubService(serviceName, identifier,
                                currentCsvPath, false);
                    } else {
                        currentCsv = platformManager.getFileFromService(serviceName, currentCsvPath,
                                false);
                    }

                    if (currentCsv != null) {
                        final byte[] csvByteArray = IOUtils.toByteArray(currentCsv);
                        csvFiles.put(csvName, csvByteArray);

                        // Save in folder
                        String path = eusFilesService.getEusQoeFilesPath(sessionManager);
                        String newFileName = sessionManager.getIdForFiles() + "-" + csvName;
                        eusFilesService.saveByteArrayFileToPathInEUS(path, newFileName,
                                csvByteArray);
                        currentCsv.close();
                    }
                }
            }
        }

        return csvFiles;
    }

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

    public Map<String, byte[]> getQoEMetricsCSV(SessionManager sessionManager, String identifier) {
        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);
        return webRTCQoEMeter.getCsvs();
    }

    public Map<String, Double> getQoEAverageMetrics(SessionManager sessionManager,
            String identifier, boolean storeInFolder) throws Exception {
        Map<String, Double> metrics = new HashMap<String, Double>();
        Map<String, byte[]> csvs = getQoEMetricsCSV(sessionManager, identifier);

        if (csvs != null) {
            for (HashMap.Entry<String, byte[]> csv : csvs.entrySet()) {
                Double average = null;

                if (csv.getKey().toLowerCase().contains("vmaf")) {
                    average = this.getVMAFAverageMetricByCsv(csv.getValue());
                } else {
                    average = this.getNonVMAFAverageMetricByCsv(csv.getValue());
                }

                String name = sessionManager.getIdForFiles() + "-" + csv.getKey().split("\\.")[0]
                        + "-average.txt";
                metrics.put(name, average);

                if (storeInFolder) {
                    // Save in folder
                    String path = eusFilesService.getEusQoeFilesPath(sessionManager);
                    eusFilesService.saveStringContentToPathInEUS(path, name,
                            String.valueOf(average));
                }
            }
        }

        return metrics;
    }

    private String getRealServiceName(SessionManager sessionManager, String identifier)
            throws Exception {
        WebRTCQoEMeter webRTCQoEMeter = getWebRTCQoEMeter(identifier);

        if (webRTCQoEMeter != null) {
            // Default docker/k8s
            String serviceName = identifier;

            // If AWS session
            if (sessionManager.isAWSSession() && webRTCQoEMeter.getAwsInstanceId() != null) {
                serviceName = webRTCQoEMeter.getAwsInstanceId();
            }
            return serviceName;
        } else {
            throw new Exception("Error on upload videos to QoE service: Identifier " + identifier
                    + " not found");
        }
    }

    private Double getVMAFAverageMetricByCsv(byte[] csv) throws IOException {
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
        return average;
    }

    private Double getNonVMAFAverageMetricByCsv(byte[] csv) throws IOException {
        Double average = 0.0;
        int total = 0;

        InputStream is = null;
        BufferedReader bfReader = null;

        is = new ByteArrayInputStream(csv);
        bfReader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        // two columns: frame and value
        while ((line = bfReader.readLine()) != null) {
            try {
                String[] lineColumns = line.split(",");
                Double valueAsNum = Double.valueOf(lineColumns[1]);
                average = average + valueAsNum;
                total++;
            } catch (Exception e) {
            }
        }

        average = average / total;
        if (is != null) {
            is.close();
        }
        return average;
    }

}
