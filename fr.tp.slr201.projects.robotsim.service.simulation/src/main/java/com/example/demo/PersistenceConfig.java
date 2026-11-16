package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.RemoteFileCanvasChooser;

@Configuration
public class PersistenceConfig {

    @Bean
    public RemoteFactoryPersistenceManager remoteFactoryPersistenceManager() {
    	final RemoteFileCanvasChooser canvasChooser = new RemoteFileCanvasChooser("factory", "Puck Factory");
        return new RemoteFactoryPersistenceManager(canvasChooser);
    }
}