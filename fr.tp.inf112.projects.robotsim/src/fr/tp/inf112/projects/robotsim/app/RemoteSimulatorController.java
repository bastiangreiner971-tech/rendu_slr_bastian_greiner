package fr.tp.inf112.projects.robotsim.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactorySimulationEventConsumer;
import fr.tp.inf112.projects.robotsim.model.LocalFactoryModelChangedNotifier;
import tools.jackson.databind.ObjectMapper;

public class RemoteSimulatorController extends SimulatorController {
	HttpClient httpClient = HttpClient.newHttpClient();
	ObjectMapper objectMapper = new ObjectMapper();
	final static Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());
	private final LocalFactoryModelChangedNotifier viewNotifier =
	        new LocalFactoryModelChangedNotifier();
	public FactorySimulationEventConsumer eventConsumer;
	public String factoryId;
	
	public RemoteSimulatorController(final CanvasPersistenceManager persistenceManager) {
		super(persistenceManager);
	}
	
	public RemoteSimulatorController(final Factory factoryModel,
							   final CanvasPersistenceManager persistenceManager) {
		super(factoryModel, persistenceManager);
		
	}

	@Override
	public void startAnimation() {
		final Factory factory = (Factory) getCanvas();
		if (factory == null) {
			LOGGER.warning("Cannot start simulation: no factory is loaded.");
			return;
		}

		if (factory.getId() == null || factory.getId().isBlank()) {
			final String canvasId;
			try {
				canvasId = chooseNewCanvasId();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Could not choose a file name for the factory.", e);
				return;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}

			if (canvasId == null || canvasId.isBlank()) {
				return;
			}

			factory.setId(canvasId);
			try {
				getPersistenceManager().persist(factory);
			} catch (IOException e) {
				factory.setId(null);
				LOGGER.log(Level.SEVERE, "Could not save the factory; simulation was not started.", e);
				return;
			}
		}

		factoryId = factory.getId();
		try {
			final URI uri = new URI("http", null, "localhost", 8080, "/simulation/start/" + factoryId, null, null);
			final HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
			final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (Boolean.parseBoolean(response.body())) {
				factory.setSimulationStarted(true);
				eventConsumer = new FactorySimulationEventConsumer(this);
				final Thread consumerThread = new Thread(eventConsumer::consumeMessages, "factory-events");
				consumerThread.start();
			} else {
				LOGGER.warning("The microservice refused to start the simulation for '" + factoryId + "'.");
			}
		} catch (URISyntaxException | IOException e) {
			LOGGER.log(Level.SEVERE, "Could not start the remote simulation.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.log(Level.WARNING, "Interrupted while starting the remote simulation.", e);
		}
	}

	private String chooseNewCanvasId() throws IOException, InterruptedException {
		final CanvasChooser chooser = getPersistenceManager().getCanvasChooser();
		if (chooser == null) {
			throw new IOException("No canvas chooser is configured.");
		}

		if (SwingUtilities.isEventDispatchThread()) {
			return chooser.newCanvasId();
		}

		final AtomicReference<String> canvasId = new AtomicReference<>();
		final AtomicReference<IOException> chooserException = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					canvasId.set(chooser.newCanvasId());
				} catch (IOException e) {
					chooserException.set(e);
				}
			});
		} catch (InvocationTargetException e) {
			throw new IOException("Could not open the save dialog.", e.getCause());
		}

		if (chooserException.get() != null) {
			throw chooserException.get();
		}
		return canvasId.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopAnimation() {
		//factoryModel.stopSimulation();
		try{
			final URI uri = new URI("http", null, "localhost", 8080, "/simulation/stop/"+factoryId, null, null);
			HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (Boolean.parseBoolean(response.body())) {
			    Factory localFactory = (Factory) getCanvas();
			    if (localFactory != null) {
			        localFactory.setSimulationStarted(false);
			        viewNotifier.notifyObservers();
			    }
			    eventConsumer = null;
			}
		}catch(URISyntaxException | IOException | InterruptedException e) {
			
		}
	}
	
	private Factory getFactory() {
		try{
			final URI uri = new URI("http", null, "localhost", 8080, "/simulation/get/"+factoryId, null, null);
			HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.body().length()==0) {
				return null;
			}
			Factory remoteFactoryModel = objectMapper.readValue(response.body(), Factory.class);
			//LOGGER.info("recup succes");
			return remoteFactoryModel;
		}catch(URISyntaxException | IOException | InterruptedException e) {
			LOGGER.info("recup fail");
			LOGGER.info(e.getMessage());
			return null;
		}
	}
	@Override
	public boolean addObserver(Observer observer) {
	    return viewNotifier.addObserver(observer);
	}

	@Override
	public boolean removeObserver(Observer observer) {
	    return viewNotifier.removeObserver(observer);
	}
	@Override
	public void setCanvas(final Canvas canvasModel) {
		Factory factory = (Factory) canvasModel;

	    SwingUtilities.invokeLater(() -> {
	        super.setCanvas(factory);
	        viewNotifier.notifyObservers();
	    });
	}

	
}
