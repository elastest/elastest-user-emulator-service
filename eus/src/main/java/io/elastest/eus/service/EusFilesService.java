package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.elastest.eus.api.model.ExecutionData;

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

    @Value("${host.shared.files.relative.folder}")
    private String hostSharedFilesRelativeFolder;

    public static final String FILE_SEPARATOR = "/";

    public EusFilesService() {
    }

    public String getEusFilesPath() {
        return eusFilesPath;
    }

    public String getInternalSessionFolderFromExecution(ExecutionData data) {
        return getEusFilesPath() + data.getFolderPath();
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
    public Boolean uploadFileToPath(String path, String fileName,
            MultipartFile multipartFile)
            throws IllegalStateException, IOException {
        // Create folder if not exist
        createFolderIfNotExists(path);

        File file = new File(path + fileName);
        if (file.exists()) {
            return false;
        }
        multipartFile.transferTo(file);
        return true;
    }

    public Boolean uploadFileToSession(String sessionId, String fileName,
            MultipartFile multipartFile)
            throws IllegalStateException, IOException {
        String path = getEusFilesPath();
        path = path + (path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + hostSharedFilesRelativeFolder + FILE_SEPARATOR;
        return uploadFileToPath(path, fileName, multipartFile);
    }

    public Boolean uploadFileToSession(String sessionId, MultipartFile file)
            throws IllegalStateException, IOException {
        return uploadFileToSession(sessionId, file.getOriginalFilename(), file);
    }

    public Boolean uploadFileToSessionExecution(ExecutionData data,
            String sessionId, String fileName, MultipartFile multipartFile)
            throws IllegalStateException, IOException {
        String path = getInternalSessionFolderFromExecution(data);
        path = path + (path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + hostSharedFilesRelativeFolder + FILE_SEPARATOR;
        return uploadFileToPath(path, fileName, multipartFile);
    }

    public Boolean uploadFileToSessionExecution(ExecutionData data,
            String sessionId, MultipartFile file)
            throws IllegalStateException, IOException {
        return uploadFileToSessionExecution(data, sessionId,
                file.getOriginalFilename(), file);
    }
}
