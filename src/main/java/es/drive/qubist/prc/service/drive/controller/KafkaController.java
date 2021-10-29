package es.drive.qubist.prc.service.drive.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.services.drive.model.File;

import es.drive.qubist.prc.service.drive.service.DriveService;

@RestController
public class KafkaController {

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaController.class);

	public static final String TOPIC = "Drive";

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@PostMapping(value = "/publish/files", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public ResponseEntity<String> setFile(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "mime") String mime, @RequestParam(name = "parent", required = false) String parent) {
		DriveService ds = null;
		try {
			ds = new DriveService();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String filename = file.getOriginalFilename();
		java.io.File tmpFile = null;
		try {
			tmpFile = java.io.File.createTempFile(filename, "." + filename.substring(filename.lastIndexOf(".") + 1));
			file.transferTo(tmpFile);
		} catch (IOException | IllegalStateException e) {
			e.printStackTrace();
		}
		File fileDrive = ds.setFile(tmpFile, mime, filename, parent);
		MultiValueMap<String, Object> data = new LinkedMultiValueMap<String, Object>();
		data.add("tmpFile", tmpFile);
		data.add("file", filename);
		data.add("mime", mime);
		data.add("parent", parent);
		LOGGER.info("Publishing to topic " + TOPIC);
		this.kafkaTemplate.send(TOPIC, data);
		return new ResponseEntity<String>("Created", HttpStatus.OK);
	}

}
