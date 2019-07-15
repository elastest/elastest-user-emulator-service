package io.elastest.eus.platform.manager;

import static java.lang.System.currentTimeMillis;
import static java.lang.invoke.MethodHandles.lookup;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;

import com.spotify.docker.client.exceptions.DockerException;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.ContextProperties;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.session.SessionManager;

public abstract class PlatformManager {
    final Logger logger = getLogger(lookup().lookupClass());

    protected ContextProperties contextProperties;
    protected EusFilesService eusFilesService;

    public PlatformManager(EusFilesService eusFilesService,
            ContextProperties contextProperties) {
        super();
        this.eusFilesService = eusFilesService;
        this.contextProperties = contextProperties;
    }

    public abstract InputStream getFileFromBrowser(
            SessionManager sessionManager, String path, Boolean isDirectory)
            throws Exception;

    public abstract void copyFilesFromBrowserIfNecessary(
            SessionManager sessionManager) throws IOException;

    public abstract String getSessionContextInfo(SessionManager sessionManager)
            throws Exception;

    public String generateRandomContainerNameWithPrefix(String prefix) {
        return prefix + randomUUID().toString();
    }

    public abstract void buildAndRunBrowserInContainer(
            SessionManager sessionManager, String containerPrefix,
            String originalRequestBody, String folderPath,
            ExecutionData execData, List<String> envs,
            Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception;

    public abstract void execCommand(String hubContainerName,
            boolean awaitCompletion, String... command) throws Exception;

    public abstract boolean existServiceWithName(String name) throws Exception;

    public abstract void removeServiceWithTimeout(String containerId,
            int killAfterSeconds) throws Exception;

    public abstract void waitForBrowserReady(String internalVncUrl,
            SessionManager sessionManager) throws Exception;

    public abstract BrowserSync buildAndRunBrowsersyncService(
            ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception;

    /* *************************************** */
    /* ********* Implemented Methods ********* */
    /* *************************************** */

    @SuppressWarnings("static-access")
    protected String createRecordingsPath(String hostPath) {
        logger.debug("Creating recordings path from: {}", hostPath);
        String recordingsPath = "";
        String pathRecordingsInHost = hostPath
                + (hostPath.endsWith(EusFilesService.FILE_SEPARATOR) ? ""
                        : EusFilesService.FILE_SEPARATOR);
        String recordingsRelativePath = pathRecordingsInHost
                .substring(
                        pathRecordingsInHost
                                .indexOf(eusFilesService.FILE_SEPARATOR,
                                        pathRecordingsInHost.indexOf(
                                                eusFilesService.FILE_SEPARATOR)
                                                + 1));
        recordingsPath = eusFilesService.getEtSharedFolder()
                + recordingsRelativePath;

        return recordingsPath;
    }

    protected void moveFiles(File fileToMove, String targetPath)
            throws IOException {
        if (fileToMove.isDirectory()) {
            for (File file : fileToMove.listFiles()) {
                moveFiles(file, targetPath + "/" + file.getName());
            }
        } else {
            try {
                Files.move(Paths.get(fileToMove.getPath()),
                        Paths.get(targetPath),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Error moving files to other directory.");
                throw e;
            }
        }

    }

    private void waitUrl(String url, long timeoutMillis, long endTimeMillis,
            String errorMessage, int pollTimeMs)
            throws IOException, InterruptedException, DockerException {
        int responseCode = 0;
        while (true) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url)
                        .openConnection();
                connection.setConnectTimeout((int) timeoutMillis);
                connection.setReadTimeout((int) timeoutMillis);
                connection.setRequestMethod("GET");
                responseCode = connection.getResponseCode();

                if (responseCode == HTTP_OK) {
                    logger.debug("URL {} already reachable", url);
                    break;
                } else {
                    logger.trace(
                            "URL {} not reachable (response {}). Trying again in {} ms",
                            url, responseCode, pollTimeMs);
                }

            } catch (SSLHandshakeException | SocketException e) {
                logger.trace("Error {} waiting URL {}, trying again in {} ms",
                        e.getMessage(), url, pollTimeMs);

            } finally {
                // Polling to wait a consistent state
                try {
                    Thread.sleep(pollTimeMs);
                } catch (InterruptedException e) {
                    logger.warn("Thread waiting interrupted: {}",
                            e.getMessage());
                }
            }

            if (currentTimeMillis() > endTimeMillis) {
                throw new DockerException(errorMessage);
            }
        }
    }

    public void waitForHostIsReachable(String url, int waitTimeoutSec)
            throws DockerException {
        long timeoutMillis = MILLISECONDS.convert(waitTimeoutSec, SECONDS);
        long endTimeMillis = System.currentTimeMillis() + timeoutMillis;

        logger.debug("Waiting for {} to be reachable (timeout {} seconds)", url,
                waitTimeoutSec);
        String errorMessage = "URL " + url + " not reachable in "
                + waitTimeoutSec + " seconds";
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[] {};
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs,
                            String authType) {
                        // No actions required
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs,
                            String authType) {
                        // No actions required
                    }
                } };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            waitUrl(url, timeoutMillis, endTimeMillis, errorMessage, 200);

        } catch (Exception e) {
            // Not propagating multiple exceptions (NoSuchAlgorithmException,
            // KeyManagementException, IOException, InterruptedException) to
            // improve readability
            throw new DockerException(errorMessage, e);
        }

    }

    @Override
    public String toString() {
        return "";
    }
}
