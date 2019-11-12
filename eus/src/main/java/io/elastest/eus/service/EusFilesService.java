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
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

    // Internal for eus (/data/eus)
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

    @Value("${container.shared.files.folder}")
    public String CONTAINER_SHARED_FILES_FOLDER;

    @Value("${qoe.files.relative.folder}")
    private String qoeFilesRelativeFolder;

    public static final String FILE_SEPARATOR = "/";

    public EusFilesService() {
    }

    public String getEtSharedFolder() {
        return etSharedFolder;
    }

    public String getEusFilesPath() {
        return eusFilesPath;
    }

    public String getEtDataInHostPath() {
        return etDataInHost;
    }

    public String getInternalSessionFolderFromExecution(ExecutionData data) {
        return getEtSharedFolder() + data.getFolderPath();
    }

    // If live session, eus path, if execution, exec/eus path
    // path into EUS container (like /data/..., not ~/.elastest)
    public String getSessionFilesFolderBySessionManager(SessionManager sessionManager) {
        String filesFolder = this.getEusFilesPath();

        if (sessionManager.isSessionFromExecution()) {
            filesFolder = this.getInternalSessionFolderFromExecution(
                    sessionManager.getElastestExecutionData());
        }
        return filesFolder;
    }

    public String getEusQoeFilesPath(SessionManager sessionManager) {
        String path = getSessionFilesFolderBySessionManager(sessionManager);
        path = path + (path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR) + qoeFilesRelativeFolder
                + FILE_SEPARATOR;
        return path;
    }

    // path into EUS container (like /data/..., not ~/.elastest)
    public String getEusSharedFilesPath(SessionManager sessionManager) {
        String path = getSessionFilesFolderBySessionManager(sessionManager);
        path = path + (path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + hostSharedFilesRelativeFolder + FILE_SEPARATOR;
        return path;
    }

    public String getInternalSharedFilesPath(SessionManager sessionManager) {
        return CONTAINER_SHARED_FILES_FOLDER + FILE_SEPARATOR;
    }

    // in host (~/.elastest/eus)
    public String getFilesPathInHostPath() {
        return filesPathInHost;
    }

    // in host (~/.elastest/tjob.../eus)
    public String getHostSessionFolderFromExecution(ExecutionData data) {
        return getEtDataInHostPath() + data.getFolderPath();
    }

    // in host (~/.elastest/...)
    public String getHostSessionFolderFromSession(SessionManager sessionManager) {
        String folder = this.getFilesPathInHostPath();

        if (sessionManager.isSessionFromExecution()) {
            folder = this
                    .getHostSessionFolderFromExecution(sessionManager.getElastestExecutionData());
        }
        return folder;
    }

    // path in host (like ~/.elastest/eus/shared_files or ~/.elastest/tjob.../eus/shared_files)
    public String getHostSharedFilesPath(SessionManager sessionManager) {
        String path = getHostSessionFolderFromSession(sessionManager);
        path = path + (path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + hostSharedFilesRelativeFolder + FILE_SEPARATOR;
        return path;
    }

    public void createFolderIfNotExists(String path) {
        File folderStructure = new File(path);

        if (!folderStructure.exists()) {
            logger.debug("Try to create folder structure: {}", path);
            logger.info("Creating folder at {}.", folderStructure.getAbsolutePath());
            boolean created = folderStructure.mkdirs();
            if (!created) {
                logger.error("Folder does not created at {}.", path);
                return;
            }
            logger.info("Folder created at {}.", path);
        }
    }

    // Generic
    public Boolean saveFileToPathInEUS(String path, String fileName, MultipartFile multipartFile)
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

    public Boolean saveInputStreamFileToPathInEUS(String path, String fileName,
            InputStream inpuStreamFile) throws IllegalStateException, IOException {
        // Create folder if not exist
        createFolderIfNotExists(path);

        path = path.endsWith("/") ? path : path + "/";

        File file = new File(path + fileName);
        if (file.exists()) {
            return false;
        }
        Files.copy(inpuStreamFile, file.toPath());

        IOUtils.closeQuietly(inpuStreamFile);
        return true;
    }

    public Boolean saveByteArrayFileToPathInEUS(String path, String fileName, byte[] byteArrayFile)
            throws IllegalStateException, IOException {
        // Create folder if not exist
        createFolderIfNotExists(path);

        path = path.endsWith("/") ? path : path + "/";

        File file = new File(path + fileName);
        if (file.exists()) {
            return false;
        }

        FileOutputStream os = new FileOutputStream(file);
        os.write(byteArrayFile);
        os.close();

        return true;
    }

    public Boolean saveStringContentToPathInEUS(String path, String fileName, String content)
            throws IllegalStateException, IOException {
        // Create folder if not exist
        createFolderIfNotExists(path);

        path = path.endsWith("/") ? path : path + "/";

        File file = new File(path + fileName);
        if (file.exists()) {
            return false;
        }
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        return true;
    }

    public File saveFileFromUrlToPathInEUS(String path, String fileName, String fileUrl)
            throws IllegalStateException, IOException {
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

    public File createFileFromString(String string, String targetPath) throws IOException {
        File file = new File(targetPath);
        FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8);
        return ResourceUtils.getFile(targetPath);
    }

    public File createFileFromInputStream(InputStream iStream, String targetPath)
            throws IOException {
        File file = new File(targetPath);
        FileUtils.copyInputStreamToFile(iStream, file);
        return ResourceUtils.getFile(targetPath);
    }

    public String getFileNameFromCompleteFilePath(String completeFilePath) throws Exception {
        String[] splittedFilePath = completeFilePath.split("/");
        return splittedFilePath[splittedFilePath.length - 1];
    }

    public String getPathWithoutFileNameFromCompleteFilePath(String completeFilePath)
            throws Exception {
        String[] splittedFilePath = completeFilePath.split("/");
        String finalPath = "";
        int position = 0;
        for (String pathPart : splittedFilePath) {
            if (position < splittedFilePath.length - 1) {
                if (position > 0) {
                    finalPath += "/";
                }
                finalPath += pathPart;
            }
            position++;
        }
        return finalPath;
    }

}
