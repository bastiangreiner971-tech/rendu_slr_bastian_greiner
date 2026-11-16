package com.example.demo;

import fr.tp.inf112.projects.canvas.model.Canvas;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.tp.inf112.projects.robotsim.app.RemoteSimulatorController;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Robot;
@RestController
@RequestMapping("/simulation")
public class Controller {
	@Autowired
	private KafkaTemplate<String, Factory> simulationEventTemplate;
	
	private final RemoteFactoryPersistenceManager persistenceManager;
	final static Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());

	private final Map<String, Factory> simulations = new ConcurrentHashMap<>();
	
	public Controller(RemoteFactoryPersistenceManager persistanceManager) {
		this.persistenceManager = persistanceManager;
	}

	@GetMapping("/start/{factoryId}")
	public boolean startFactory(@PathVariable String factoryId) {
		try {

			// a. Récupérer le modèle auprès du serveur de persistance
            Canvas canvas = persistenceManager.read(factoryId);

            if (canvas == null) {
                return false;
            }
            if (!(canvas instanceof Factory factory)) {
                return false;
            }

			factory.setId(factoryId);

            // b. Ajouter le modèle aux simulations en cours
            simulations.put(factoryId, factory);

            // c. Démarrer la simulation
            for (Component component : factory.getComponents()) {
                if (component instanceof Robot robot) {
                    robot.initializePathFinder();
                }
            }
            final FactoryModelChangedNotifier notifier = new
            		KafkaFactoryModelChangedNotifier(factory, simulationEventTemplate);
            factory.setNotifier(notifier);
            factory.startSimulation();
            LOGGER.info(factory.getName());
            LOGGER.info(""+factory.isSimulationStarted());
            // d. Tout s'est bien passé
            return true;

        } catch (IOException e) {

            e.printStackTrace();
            return false;
        }
    }
	@GetMapping("/get/{factoryId}")
	public Factory getFactory(@PathVariable String factoryId) {
		return simulations.get(factoryId);
    }
	@GetMapping("/stop/{factoryId}")
	public boolean stopFactory(@PathVariable String factoryId) {
		// a. Récupérer le modèle auprès du serveur de persistance
		Factory factory = simulations.get(factoryId);
		
		if (factory == null) {
		    return false;
		}
		// b. Stoper la simulation
		factory.stopSimulation();
		
		// c. Retirer le modèle des simulations en cours
		simulations.remove(factoryId);

 

		// d. Tout s'est bien passé
		return true;
    }
}

