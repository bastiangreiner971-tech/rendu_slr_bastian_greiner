package com.example.demo;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.SimulationServiceUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class SimulationServiceConfig {
	@Bean
	@Primary
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
	@Bean
	ProducerFactory<String, Factory> producerFactory(){
		final Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SimulationServiceUtils.BOOTSTRAP_SERVERS);
		final JsonSerializer<Factory> factorySerializer = new JsonSerializer<>(objectMapper());
		return new DefaultKafkaProducerFactory<>(config,
				new StringSerializer(),
				factorySerializer);
	}
	@Bean
	@Primary
	KafkaTemplate<String, Factory> kafkaTemplate(){
		return new KafkaTemplate<>(producerFactory());
	}
}