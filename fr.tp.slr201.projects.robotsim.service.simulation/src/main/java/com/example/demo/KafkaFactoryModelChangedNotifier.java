package com.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;


import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.SimulationServiceUtils;

public class KafkaFactoryModelChangedNotifier implements FactoryModelChangedNotifier{
	private static final Logger LOGGER = Logger.getLogger(KafkaFactoryModelChangedNotifier.class.getName());
	
	
	private List<Observer> observers;
	
	private Factory factoryModel;
	
	private KafkaTemplate<String, Factory> simulationEventTemplate;
	
	
	public KafkaFactoryModelChangedNotifier(Factory factoryModel, KafkaTemplate<String, Factory> simulationEventTemplate) {
		this.factoryModel = factoryModel;
		this.simulationEventTemplate = simulationEventTemplate;
		observers = null;
	}
	@Override
	public void notifyObservers() {
		final String topicName = SimulationServiceUtils.getTopicName(factoryModel);
		final boolean simulationStarted = factoryModel.isSimulationStarted();
		LOGGER.info("Publishing factory event: id=" + factoryModel.getId()
				+ ", simulationStarted=" + simulationStarted + ", topic=" + topicName);
		final Message<Factory> factoryMessage = MessageBuilder.withPayload(factoryModel)
				.setHeader(KafkaHeaders.TOPIC, topicName)
				.build();
		final CompletableFuture<SendResult<String, Factory>> sendResult =
		simulationEventTemplate.send(factoryMessage);
		sendResult.whenComplete((result, ex) -> {
			if (ex != null) {
				LOGGER.log(Level.SEVERE, "Kafka publish failed for topic '" + topicName + "'.", ex);
			} else {
				LOGGER.info("Kafka event published: topic=" + topicName
						+ ", simulationStarted=" + simulationStarted
						+ ", offset=" + result.getRecordMetadata().offset());
			}
		});
	}

	@Override
	public List<Observer> getObservers() {
		if (observers == null) {
			observers = new ArrayList<>();
		}
		
		return observers;
	}
	
	@Override
	public boolean addObserver(Observer observer) {
		return getObservers().add(observer);
	}
	@Override
	public boolean removeObserver(Observer observer) {
		return getObservers().remove(observer);
	}
	

}
