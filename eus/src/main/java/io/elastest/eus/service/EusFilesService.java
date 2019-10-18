package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.session.SessionManager;

@Service
public class EusFilesService {
    final Logger logger = getLogger(lookup().lookupClass());

    @Value("${et.shared.folder}")
    private String etSharedFolder;

    // Internal (/data/eus)
    @Value("${et.files.path}")
    private String eusFilesPath;

    // Host
    @Value("${et.files.path.in.host}")
    private String filesPathInHost;

    // Host (without /eus)
    @Value("${et.data.in.host}")
    private String etDataInHost;

    @Value("${container.main.folder}")
    public String CONTAINER_MAIN_FOLDER;

    @Value("${host.shared.files.relative.folder}")
    private String hostSharedFilesRelativeFolder;

    public static final String FILE_SEPARATOR = "/";

    public EusFilesService() {
    }

    public String getEtSharedFolder() {
        return etSharedFolder;
    }

    public String getEusFilesPath() {
        return eusFilesPath;
    }

    public String getInternalSessionFolderFromExecution(ExecutionData data) {
        return getEtSharedFolder() + data.getFolderPath();
    }

    // If live session, eus path, if execution, exec/eus path
    public String getSessionFilesFolderBySessionManager(
            SessionManager sessionManager) {
        String filesFolder = this.getEusFilesPath();

        if (sessionManager.isSessionFromExecution()) {
            filesFolder = this.getInternalSessionFolderFromExecution(
                    sessionManager.getElastestExecutionData());
        }
        return filesFolder;
    }

    public String getEusSharedFilesPath(SessionManager sessionManager) {
        String path = getSessionFilesFolderBySessionManager(sessionManager);
        path = path + (path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + hostSharedFilesRelativeFolder + FILE_SEPARATOR;
        return path;
    }

    public String getInternalSharedFilesPath(SessionManager sessionManager) {
        return CONTAINER_MAIN_FOLDER + FILE_SEPARATOR
                + hostSharedFilesRelativeFolder + FILE_SEPARATOR;
    }

    public String getFilesPathInHostPath() {
        return filesPathInHost;
    }

    public String getEtDataInHostPath() {
        return etDataInHost;
    }

    public String getHostSessionFolderFromExecution(ExecutionData data) {
        return getEtDataInHostPath() + data.getFolderPath();
    }

    public void createFolderIfNotExists(String path) {
        File folderStructure = new File(path);

        if (!folderStructure.exists()) {
            logger.debug("Try to create folder structure: {}", path);
            logger.info("Creating folder at {}.",
                    folderStructure.getAbsolutePath());
            boolean created = folderStructure.mkdirs();
            if (!created) {
                logger.error("Folder does not created at {}.", path);
                return;
            }
            logger.info("Folder created at {}.", path);
        }
    }

    // Generic
    public Boolean saveFileToPathInEUS(String path, String fileName,
            MultipartFile multipartFile)
            throws IllegalStateException, IOException {
        // Create folder if not exist
        createFolderIfNotExists(path);

        path = path.endsWith("/") ? path : path + "/";

        File file = new File(path + fileName);
        if (file.exists()) {
            return false;
        }
        multipartFile.transferTo(file);
        return true;
    }

    public File saveFileFromUrlToPathInEUS(String path, String fileName,
            String fileUrl) throws IllegalStateException, IOException {
        // Create folder if not exist
        createFolderIfNotExists(path);
        URL url = new URL(fileUrl);

        path = path.endsWith("/") ? path : path + "/";

        ReadableByteChannel readChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOS = new FileOutputStream(path + fileName);
        FileChannel writeChannel = fileOS.getChannel();
        writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
        File targetFile = new File(path + fileName);

        return targetFile;
    }

    public File createFileFromString(String string, String targetPath)
            throws IOException {
        File file = new File(targetPath);
        FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8);
        return ResourceUtils.getFile(targetPath);
    }

    public File createFileFromInputStream(InputStream iStream,
            String targetPath) throws IOException {
        File file = new File(targetPath);
        FileUtils.copyInputStreamToFile(iStream, file);
        return ResourceUtils.getFile(targetPath);
    }
}
