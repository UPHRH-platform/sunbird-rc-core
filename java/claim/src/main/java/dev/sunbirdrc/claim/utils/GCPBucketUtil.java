
package dev.sunbirdrc.claim.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import dev.sunbirdrc.claim.dto.FileDto;
import dev.sunbirdrc.claim.exception.BadRequestException;
import dev.sunbirdrc.claim.exception.FileWriteException;
import dev.sunbirdrc.claim.exception.GCPFileUploadException;
import dev.sunbirdrc.claim.exception.InvalidFileTypeException;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class GCPBucketUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GCPBucketUtil.class);

    @Value("${gcp.config.file}")
    private String gcpConfigFile;

    @Value("${gcp.project.id}")
    private String gcpProjectId;

    @Value("${gcp.bucket.id}")
    private String gcpBucketId;

    @Value("${gcp.dir.name}")
    private String gcpDirectoryName;

    @Value("${gcp.file.validity}")
    private Integer validity;


    public FileDto uploadFile(MultipartFile multipartFile, String fileName, String contentType) {

        try{

            LOGGER.debug("Start file uploading process on GCS");
            byte[] fileData = FileUtils.readFileToByteArray(convertFile(multipartFile));

            InputStream inputStream = new ClassPathResource(gcpConfigFile).getInputStream();

            StorageOptions options = StorageOptions.newBuilder().setProjectId(gcpProjectId)
                    .setCredentials(GoogleCredentials.fromStream(inputStream)).build();
            Storage storage = options.getService();

            Bucket bucket = storage.get(gcpBucketId,Storage.BucketGetOption.fields());

            Acl acl = Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER);
            List aclList = new ArrayList();
            aclList.add(acl);

            Blob blob = bucket.create(gcpDirectoryName + "/" + fileName, fileData, contentType);
            LOGGER.debug("Storing GCS file:"+fileName);
            validity=validity > 30 ? 30 : validity;
            //URL url =  blob.signUrl(validity, TimeUnit.DAYS,Storage.SignUrlOption.withV4Signature());
            String link = blob.getMediaLink();
            //String fileUrl = url.toString();
            String url2 = "https://storage.googleapis.com/dev-public-upsmf/issuance/"+fileName;
            LOGGER.debug("File url of GCS: "+link);

            if(blob != null){
                LOGGER.debug("File successfully uploaded to GCS");
                return new FileDto(blob.getName(), url2);
            }

        }catch (GCPFileUploadException e){
            LOGGER.error("An error occurred while uploading data. Exception: ", e);
            throw new GCPFileUploadException("An error occurred while storing data to GCS");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new GCPFileUploadException("An error occurred while storing data to GCS");
    }

    public Blob DownloadFile(String fileName) {
        try{
            LOGGER.debug("Start file Download process on GCS");
            InputStream inputStream = new ClassPathResource(gcpConfigFile).getInputStream();

            StorageOptions options = StorageOptions.newBuilder().setProjectId(gcpProjectId)
                    .setCredentials(GoogleCredentials.fromStream(inputStream)).build();

            Storage storage = options.getService();
            Bucket bucket = storage.get(gcpBucketId,Storage.BucketGetOption.fields());

            RandomString id = new RandomString();
            Blob blob = bucket.get(fileName);
            return blob;
        }catch (GCPFileUploadException e){
            LOGGER.error("An error occurred while Downloading data. Exception: ", e);
            throw new GCPFileUploadException("An error occurred while Downloading from GCS");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private File convertFile(MultipartFile file) {
        FileOutputStream outputStream = null;
        try{
            if(file.getOriginalFilename() == null){
                throw new BadRequestException("Original file name is null");
            }
            File convertedFile = new File(file.getOriginalFilename());
            outputStream = new FileOutputStream(convertedFile);
            outputStream.write(file.getBytes());
            LOGGER.debug("Converting multipart file : {}", convertedFile);
            return convertedFile;
        }catch (Exception e){
            throw new FileWriteException("An error has occurred while converting the file");
        }finally {
            if(outputStream!=null)
            try {
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private String checkFileExtension(String fileName) {
        if(fileName != null && fileName.contains(".")){
            String[] extensionList = {".png", ".jpeg", ".pdf",".PDF", ".doc", ".mp3"};

            for(String extension: extensionList) {
                if (fileName.endsWith(extension)) {
                    LOGGER.debug("Accepted file type : {}", extension);
                    return extension;
                }
            }
        }
        LOGGER.error("Not a permitted file type");
        throw new InvalidFileTypeException("Not a permitted file type");
    }

}

