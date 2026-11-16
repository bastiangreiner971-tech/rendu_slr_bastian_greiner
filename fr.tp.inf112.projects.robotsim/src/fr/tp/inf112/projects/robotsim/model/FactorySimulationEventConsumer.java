package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;



import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.tp.inf112.projects.robotsim.app.RemoteSimulatorController;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Factory;

public class FactorySimulationEventConsumer{
	
	private final KafkaConsumer<String, String> consumer;
	private final RemoteSimulatorController controller;
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private static final Logger LOGGER = Logger.getLogger(FactorySimulationEventConsumer.class.getName());
	
	public FactorySimulationEventConsumer(final RemoteSimulatorController controller) {
		this.controller = controller;
		final Properties props = SimulationServiceUtils.getDefaultConsumerProperties();
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class);
		this.consumer = new KafkaConsumer<>(props);
		final String topicName = SimulationServiceUtils.getTopicName(controller.factoryId);
		this.consumer.subscribe(Collections.singletonList(topicName));
		LOGGER.info("Subscribed to Kafka topic '" + topicName + "'.");
	}
	
	public void consumeMessages() {
		try {
			while (controller.isAnimationRunning()) {
				final ConsumerRecords<String, String> records =
						consumer.poll(Duration.ofMillis(100));
				for (final ConsumerRecord<String, String> record : records) {
					LOGGER.info("Received Kafka event: topic=" + record.topic()
							+ ", offset=" + record.offset() + ", timestamp=" + record.timestamp());
					try {
						Factory factory = objectMapper.readValue(record.value(), Factory.class);
						LOGGER.info("Decoded factory state: id=" + factory.getId()
								+ ", simulationStarted=" + factory.isSimulationStarted());
		                   	controller.setCanvas(factory);
		                } catch (IOException e) {
		                    LOGGER.warning("JSON Factory invalide : " + e.getMessage());
		                }
				}
			}
			LOGGER.info("Consumer loop ended; animationRunning=" + controller.isAnimationRunning());
		}
		finally {
			consumer.close();
		}
	}
}