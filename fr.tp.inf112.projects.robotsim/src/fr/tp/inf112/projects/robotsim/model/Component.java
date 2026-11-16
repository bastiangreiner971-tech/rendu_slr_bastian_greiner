package fr.tp.inf112.projects.robotsim.model;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.canvas.model.Shape;
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.PROPERTY,
	    property = "type"
	)
@JsonSubTypes({
	@JsonSubTypes.Type(value = Room.class, name = "room"),
	@JsonSubTypes.Type(value = Area.class, name = "area"),
	@JsonSubTypes.Type(value = Machine.class, name = "machine"),
	@JsonSubTypes.Type(value = Robot.class, name = "robot"),
	@JsonSubTypes.Type(value = Conveyor.class, name = "conveyor"),
	@JsonSubTypes.Type(value = ChargingStation.class, name = "chargingStation"),
	@JsonSubTypes.Type(value = Door.class, name = "door")
})
public abstract class Component implements Figure, Serializable, Runnable {
	
	private static final long serialVersionUID = -5960950869184030220L;

	private String id;

	@JsonBackReference("factory-components")
	private final Factory factory;
	
	private PositionedShape positionedShape;
	
	private String name;
	
	final static Logger LOGGER = Logger.getLogger(Component.class.getName());

	protected Component(final Factory factory,
						final PositionedShape shape,
						final String name) {
		this.factory = factory;
		this.positionedShape = shape;
		this.name = name;

		if (factory != null) {
			factory.addComponent(this);
		}
	}
	protected Component() {
		this(null, null, null);
	}
	public void loginfo(String msg) {
		LOGGER.info(msg);
	}
	@Override
	public void run() {
		LOGGER.info(() -> getClass().getSimpleName() + " '" + getName()
				+ "' thread started at (" + getxCoordinate() + ", " + getyCoordinate()
				+ "), simulationStarted=" + isSimulationStarted());
		boolean exceptionLogged = false;
		while(isSimulationStarted()) {
			try {
				behave();
				Thread.sleep(50);
			}catch (Exception ex) {
				if (!exceptionLogged) {
					LOGGER.log(Level.SEVERE, "Error while running " + getClass().getSimpleName()
							+ " '" + getName() + "' at (" + getxCoordinate() + ", "
							+ getyCoordinate() + ")", ex);
					exceptionLogged = true;
				}
			}
		}
		LOGGER.info(() -> getClass().getSimpleName() + " '" + getName() + "' thread stopped");
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public PositionedShape getPositionedShape() {
		return positionedShape;
	}
	public void setPositionedShape(PositionedShape shape) {
		positionedShape = shape;
	}
	
	@JsonIgnore
	public Position getPosition() {
		final PositionedShape shape = getPositionedShape();
		return shape == null ? null : shape.getPosition();
	}

	protected Factory getFactory() {
		return factory;
	}

	@Override
	@JsonIgnore
	public int getxCoordinate() {
		final PositionedShape shape = getPositionedShape();
		return shape == null ? -1 : shape.getxCoordinate();
	}

	protected boolean setxCoordinate(int xCoordinate) {
		final PositionedShape shape = getPositionedShape();
		if (shape != null && shape.setxCoordinate(xCoordinate)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	@Override
	@JsonIgnore
	public int getyCoordinate() {
		final PositionedShape shape = getPositionedShape();
		return shape == null ? -1 : shape.getyCoordinate();
	}

	protected boolean setyCoordinate(final int yCoordinate) {
		final PositionedShape shape = getPositionedShape();
		if (shape != null && shape.setyCoordinate(yCoordinate)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	protected void notifyObservers() {
		final Factory componentFactory = getFactory();
		if (componentFactory != null) {
			componentFactory.notifyObservers();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [name=" + name + " xCoordinate=" + getxCoordinate() + ", yCoordinate=" + getyCoordinate()
				+ ", shape=" + getPositionedShape();
	}

	@JsonIgnore
	public int getWidth() {
		final PositionedShape shape = getPositionedShape();
		return shape == null ? 0 : shape.getWidth();
	}

	@JsonIgnore
	public int getHeight() {
		final PositionedShape shape = getPositionedShape();
		return shape == null ? 0 : shape.getHeight();
	}
	
	public boolean behave() {
		return false;
	}
	
	@JsonIgnore
	public boolean isMobile() {
		return false;
	}
	
	public boolean overlays(final Component component) {
		return component != null && overlays(component.getPositionedShape());
	}

	public boolean overlays(final PositionedShape shape) {
		final PositionedShape componentShape = getPositionedShape();
		return componentShape != null && shape != null && componentShape.overlays(shape);
	}
	
	public boolean canBeOverlayed(final PositionedShape shape) {
		return false;
	}
	
	@Override
	@JsonIgnore
	public Style getStyle() {
		return ComponentStyle.DEFAULT;
	}
	
	@Override
	@JsonIgnore
	public Shape getShape() {
		return getPositionedShape();
	}
	
	@JsonIgnore
	public boolean isSimulationStarted() {
		final Factory componentFactory = getFactory();
		return componentFactory != null && componentFactory.isSimulationStarted();
	}
}
