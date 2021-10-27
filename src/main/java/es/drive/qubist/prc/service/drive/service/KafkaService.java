package es.drive.qubist.prc.service.drive.service;

import java.io.IOException;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.services.drive.model.File;


@Service
public class KafkaService {
	
	@KafkaListener(topics = "Drive", groupId = "mygroup")
	public void consumeMessage(String message) {
		
		System.out.println("Consummed message :"+message);
		
		
	
	}

}
