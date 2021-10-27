package es.drive.qubist.prc.service.drive.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
@Service
public class DriveService {

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(DriveService.class);
    private static GoogleAuthorizationCodeFlow flow;
    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";

    public DriveService() throws IOException {
	InputStream in = DriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
	if (in == null) {
	    throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
	}
	GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
	flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
		.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH))).build();
	LOGGER.info("GOOGLE API: DONE");
    }

    private static Credential getCredentials() {
	Credential credential = null;
	try {
	    credential = flow.loadCredential(USER_IDENTIFIER_KEY);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return credential;
    }

    public String getCallback(HttpServletResponse response, String callbackUrl) {
	GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
	return url.setRedirectUri(callbackUrl).setAccessType("offline").build();
    }

    public void setAuthenticationCode(String code, String callbackUrl) throws IOException {
	GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(callbackUrl).execute();
	flow.createAndStoreCredential(response, "MY_DUMMY_USER");
    }

    public List<File> getFiles(String parent) {
	NetHttpTransport HTTP_TRANSPORT = null;
	Drive service = null;
	FileList result = null;
	try {
	    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	    service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
		    .setApplicationName(APPLICATION_NAME).build();
	    result = service.files().list().setQ("'" + parent + "' in parents and trashed = false")
		    .setFields("nextPageToken, files(id, name, mimeType, originalFilename)").execute();
	} catch (GeneralSecurityException | IOException e) {
	    e.printStackTrace();
	}
	List<File> files = result.getFiles();
	return files;
    }

    public OutputStream getFile(String id, OutputStream outputStream) {
	NetHttpTransport HTTP_TRANSPORT = null;
	outputStream = new ByteArrayOutputStream();
	Drive service = null;
	try {
	    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	    service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
		    .setApplicationName(APPLICATION_NAME).build();
	    service.files().get(id).executeMediaAndDownloadTo(outputStream);
	} catch (GeneralSecurityException | IOException e) {
	    e.printStackTrace();
	}
	return outputStream;
    }

    public File setFile(java.io.File file, String mimeType, String name, String parent) {
	NetHttpTransport HTTP_TRANSPORT = null;
	Drive service = null;
	File result = null;
	try {
	    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	    File fileMetadata = new File();
	    fileMetadata.setName(name);
	    List<String> parentList = new ArrayList<>();
	    parentList.add(parent);
	    if (parent != null && parent.length() > 0) {
		fileMetadata.setParents(parentList);
	    }
	    service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
		    .setApplicationName(APPLICATION_NAME).build();
	    FileContent mediaContent = new FileContent(mimeType, file);
	    result = service.files().create(fileMetadata, mediaContent)
		    .setFields("id, name, mimeType, originalFilename").execute();
	} catch (GeneralSecurityException | IOException e) {
	    e.printStackTrace();
	}
	return result;
    }
    
    
}