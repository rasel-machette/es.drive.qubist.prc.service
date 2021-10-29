package es.drive.qubist.prc.service.drive.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

	@KafkaListener(topics = "OCR", groupId = "mygroup")
	public void consumeMessage(String data) {

		System.out.println("Consummed Data From OCR : " + data);

	}

}
