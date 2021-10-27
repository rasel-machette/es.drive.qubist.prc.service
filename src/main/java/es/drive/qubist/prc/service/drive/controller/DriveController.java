package es.drive.qubist.prc.service.drive.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.services.drive.model.File;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

import es.drive.qubist.prc.service.drive.service.DriveService;
import org.springframework.web.bind.annotation.CrossOrigin;


@RestController
public class DriveController {
	@Value("${drive.callback.url}")
	private String callbackUrl;
	

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/files", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public ResponseEntity<String> getFiles(@RequestParam("parent") String parent) {
		DriveService ds = null;
		try {
			ds = new DriveService();
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject obj = new JSONObject();
		List<File> files = ds.getFiles(parent);
		Collections.sort(files, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));
		obj.put("files", files.toArray());
		return new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/files", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public ResponseEntity<String> setFile(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "mime") String mimeType, @RequestParam(name = "parent", required = false) String parent) {
		DriveService ds = null;
		try {
			ds = new DriveService();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String filename = file.getOriginalFilename();
		System.out.println(filename);
		java.io.File tmpFile = null;
		try {
			tmpFile = java.io.File.createTempFile(filename, "." + filename.substring(filename.lastIndexOf(".") + 1));
			file.transferTo(tmpFile);
		} catch (IOException | IllegalStateException e) {
			e.printStackTrace();
		}
		File fileDrive = ds.setFile(tmpFile, mimeType, filename, parent);
		return new ResponseEntity<String>(fileDrive.toString(), HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/callback", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public ResponseEntity<String> setAuthentication(@RequestParam(name = "code") String code) {
		boolean authorization = false;
		try {
			new DriveService().setAuthenticationCode(code, callbackUrl);
			authorization = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject obj = new JSONObject();
		obj.put("authorization", authorization);
		return new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/authentication", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public ResponseEntity<String> getAuthentication(HttpServletResponse response) throws IOException {
		DriveService ds = new DriveService();
		String url = ds.getCallback(response, callbackUrl);
		JSONObject obj = new JSONObject();
		obj.put("url", url);
		return new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/download")
	@ResponseBody
	public InputStreamResource getFile(@RequestParam(name = "file") String file, @RequestParam(name = "mime") String mime,
			@RequestParam(name = "filename") String filename, HttpServletResponse response)
			throws IOException, GeneralSecurityException {
		DriveService ds = new DriveService();
		OutputStream os = ds.getFile(file, response.getOutputStream());
		response.setContentType(mime);
		response.setHeader("Contect-Disposition", "attachment; filename=\"" + filename + "\"");
		ByteArrayOutputStream buffer = (ByteArrayOutputStream) os;
		byte[] bytes = buffer.toByteArray();
		InputStream inputStream = new ByteArrayInputStream(bytes);
		InputStreamResource resource = new InputStreamResource(inputStream);
		return resource;
	}
	
		
}
