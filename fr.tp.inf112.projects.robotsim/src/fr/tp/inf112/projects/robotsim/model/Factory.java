package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.tp.inf112.projects.canvas.controller.Observable;
import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Factory extends Component implements Canvas, Observable {

	private static final long serialVersionUID = 5156526483612458192L;
	
	private static final ComponentStyle DEFAULT = new ComponentStyle(5.0f);

	@JsonManagedReference("factory-components")
    private final List<Component> components;


	private boolean simulationStarted;
	
	private int pathResolution;
	
	@JsonIgnore
	private transient FactoryModelChangedNotifier notifier = new LocalFactoryModelChangedNotifier();
	@JsonIgnore
	public ReentrantLock[][] lockGrid;

	public Factory(final int width,
				   final int height,
				   final String name,
				   final int pathResolution) {
		super(null, new RectangularShape(0, 0, width, height), name);
		
		this.pathResolution = pathResolution;
		components = new ArrayList<>();
		simulationStarted = false;
		initializeLockGrid();
	}
	public Factory() {
		this(0, 0, null, 1);
	}
	public void setNotifier(FactoryModelChangedNotifier not) {
		this.notifier = not;
	}

	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		notifier = new LocalFactoryModelChangedNotifier();
	}
	
	public List<Observer> getObservers() {
		return notifier.getObservers();
	}

	@Override
	public boolean addObserver(Observer observer) {
		return notifier.addObserver(observer);
	}

	@Override
	public boolean removeObserver(Observer observer) {
		return notifier.removeObserver(observer);
	}
	
	public void notifyObservers() {
		notifier.notifyObservers();
	}
	
	public boolean addComponent(final Component component) {
		if (components.add(component)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	public boolean removeComponent(final Component component) {
		if (components.remove(component)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	public List<Component> getComponents() {
		return components;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@JsonIgnore
	public Collection<Figure> getFigures() {
		return (Collection) components;
	}
	
	public int getPathResolution() {
		return pathResolution;
	}

	public void setPathResolution(int pathResolution) {
		this.pathResolution = pathResolution;
	}

	@Override
	public String toString() {
		return super.toString() + " components=" + components + "]";
	}
	@JsonIgnore(false)
	@JsonProperty("simulationStarted")
	@Override
	public boolean isSimulationStarted() {
		return simulationStarted;
	}
	public void setSimulationStarted(boolean simulationStarted) {
	    this.simulationStarted = simulationStarted;
	}
	public void startSimulation() {
		this.loginfo("Start requested for factory '" + getName()
				+ "'; simulationStarted=" + simulationStarted
				+ ", components=" + components.size());
		if (!isSimulationStarted()) {
			if (getWidth() <= 0 || getHeight() <= 0) {
				throw new IllegalStateException("Factory dimensions must be positive before starting simulation");
			}
			initializeLockGrid();
			this.simulationStarted = true;
			notifyObservers();

			behave();
		} else {
			this.loginfo("Start ignored because factory '" + getName()
					+ "' is already marked as running");
		}
	}

	private void initializeLockGrid() {
		if (pathResolution <= 0) {
			throw new IllegalStateException("Factory path resolution must be positive");
		}

		final int gridWidth = getWidth() / pathResolution;
		final int gridHeight = getHeight() / pathResolution;
		if (gridWidth <= 0 || gridHeight <= 0) {
			lockGrid = new ReentrantLock[gridWidth][gridHeight];
			return;
		}

		if (lockGrid != null && lockGrid.length == gridWidth) {
			boolean gridIsInitialized = true;
			for (final ReentrantLock[] row : lockGrid) {
				if (row == null || row.length != gridHeight) {
					gridIsInitialized = false;
					break;
				}
				for (final ReentrantLock lock : row) {
					if (lock == null) {
						gridIsInitialized = false;
						break;
					}
				}
				if (!gridIsInitialized) {
					break;
				}
			}
			if (gridIsInitialized) {
				return;
			}
		}

		lockGrid = new ReentrantLock[gridWidth][gridHeight];
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				lockGrid[x][y] = new ReentrantLock();
			}
		}
		this.loginfo("Initialized lock grid for factory '" + getName()
				+ "' with dimensions " + gridWidth + "x" + gridHeight);
	}

	public void stopSimulation() {
		if (isSimulationStarted()) {
			this.simulationStarted = false;
			
			notifyObservers();
		}
	}

	@Override
	public boolean behave() {
		boolean behaved = true;
		
		for (final Component component : getComponents()) {
			this.loginfo(component.getClass().getSimpleName() + " '" + component.getName()
					+ "' starts at (" + component.getxCoordinate() + ", "
					+ component.getyCoordinate() + ")");
			Thread th = new Thread(component, component.getClass().getSimpleName()
					+ "-" + component.getName());
			th.start();
			//behaved = component.behave() || behaved;
		}
		
		return behaved;
	}
	
	@Override
	@JsonIgnore
	public Style getStyle() {
		return DEFAULT;
	}
	
	public boolean hasObstacleAt(final PositionedShape shape) {
		for (final Component component : getComponents()) {
			if (component.overlays(shape) && !component.canBeOverlayed(shape)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasMobileComponentAt(final PositionedShape shape,
										final Component movingComponent) {
		for (final Component component : getComponents()) {
			if (component != movingComponent && component.isMobile() && component.overlays(shape)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Component getMobileComponentAt(	final Position position,
											final Component ignoredComponent) {
		if (position == null) {
			return null;
		}
		
		return getMobileComponentAt(new RectangularShape(position.getxCoordinate(), position.getyCoordinate(), 2, 2), ignoredComponent);
	}
	
	public Component getMobileComponentAt(	final PositionedShape shape,
											final Component ignoredComponent) {
		if (shape == null) {
			return null;
		}
		
		for (final Component component : getComponents()) {
			if (component != ignoredComponent && component.isMobile() && component.overlays(shape)) {
				return component;
			}
		}
		
		return null;
	}
}
