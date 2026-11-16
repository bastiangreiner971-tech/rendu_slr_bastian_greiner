package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.CustomDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {
	
	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

	private Battery battery;
	
	private int speed;
	
	private List<Component> targetComponents;
	
	@JsonIgnore
	private transient Iterator<Component> targetComponentsIterator;
	
	private Component currTargetComponent;
	
	@JsonIgnore
	private transient Iterator<Position> currentPathPositionsIter;
	
	@JsonIgnore
	private transient boolean blocked;
	
	private Position blockedTargetPosition;
	
	private FactoryPathFinder pathFinder;

	public Robot(final Factory factory,
				 final FactoryPathFinder pathFinder,
				 final CircularShape shape,
				 final Battery battery,
				 final String name ) {
		super(factory, shape, name);
		
		this.pathFinder = pathFinder;
		
		this.battery = battery;
		
		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		blocked = false;
		blockedTargetPosition = null;
	}
	public Robot() {
		this(null, null, null, null, null);
	}
	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	public int getSpeed() {
		return speed;
	}
	public void initializePathFinder() {
	    Factory factory = getFactory();
	    pathFinder = new CustomDijkstraFactoryPathFinder(
	            factory,
	            factory.getPathResolution());
	}
	public Battery getBattery() {
		return battery;
	}

	public void setBattery(final Battery battery) {
		this.battery = battery;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}
	
	public Position getBlockedTargetPosition() {
		return blockedTargetPosition;
	}
	
	public List<Component> getTargetComponents() {
		if (targetComponents == null) {
			targetComponents = new ArrayList<>();
		}
		
		return targetComponents;
	}
	
	public boolean addTargetComponent(final Component targetComponent) {
		return getTargetComponents().add(targetComponent);
	}
	
	public boolean removeTargetComponent(final Component targetComponent) {
		return getTargetComponents().remove(targetComponent);
	}
	
	@Override
	@JsonIgnore
	public boolean isMobile() {
		return true;
	}

	@Override
	public boolean behave() {
		if (getFactory() == null || getPositionedShape() == null || pathFinder == null ||
			getTargetComponents().isEmpty()) {
			LOGGER.warning("Robot '" + getName() + "' cannot move: factory="
					+ (getFactory() != null) + ", shape=" + (getPositionedShape() != null)
					+ ", pathFinder=" + (pathFinder != null) + ", targets="
					+ getTargetComponents().size());
			return false;
		}
		
		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			currTargetComponent = nextTargetComponentToVisit();
			if (currTargetComponent == null) {
				LOGGER.warning("Robot '" + getName() + "' has no current target after selecting from "
						+ getTargetComponents().size() + " target(s)");
			} else {
				LOGGER.info("Robot '" + getName() + "' selected target '" + currTargetComponent.getName()
						+ "' at (" + currTargetComponent.getxCoordinate() + ", "
						+ currTargetComponent.getyCoordinate() + ")");
			}
			computePathToCurrentTargetComponent();
		} else if (currentPathPositionsIter == null) {
			LOGGER.info("Robot '" + getName() + "' has no restored path iterator; recalculating path to '"
					+ currTargetComponent.getName() + "'");
			computePathToCurrentTargetComponent();
		}
		
		return moveToNextPathPosition() != 0;
	}
		
	private Component nextTargetComponentToVisit() {
		if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
			targetComponentsIterator = getTargetComponents().iterator();
		}
		
		return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
	}
	
	private Position findFreeNeighbouringPosition() {
		final Position up = new Position(getPosition().getxCoordinate(), getPosition().getyCoordinate() + speed);
		final Position down = new Position(getPosition().getxCoordinate(), getPosition().getyCoordinate() - speed);
		final Position right = new Position(getPosition().getxCoordinate() + speed, getPosition().getyCoordinate());
		final Position left = new Position(getPosition().getxCoordinate() - speed, getPosition().getyCoordinate());
		if (getPosition().getyCoordinate()==blockedTargetPosition.getyCoordinate()) {
			if (isFreeNeighbouringPosition(up)) {
				//this.loginfo("1");
				return up;
			}else if (isFreeNeighbouringPosition(down)) {
				//this.loginfo("2");
				return down;
			}
		}else if (getPosition().getxCoordinate()==blockedTargetPosition.getxCoordinate()) {
			if (isFreeNeighbouringPosition(left)) {
				//this.loginfo("3");
				return left;
			}else if (isFreeNeighbouringPosition(right)) {
				//this.loginfo("4");
				return right;
			}
		}
		
		//this.loginfo("ok");
		return blockedTargetPosition;
	}

	private boolean isFreeNeighbouringPosition(final Position position) {
		final Factory factory = getFactory();
		final int factoryX = factory.getxCoordinate();
		final int factoryY = factory.getyCoordinate();
		final RectangularShape shape = new RectangularShape(
				position.getxCoordinate(), position.getyCoordinate(), getWidth(), getHeight());
		if (position.getxCoordinate() < factoryX || position.getyCoordinate() < factoryY
				|| position.getxCoordinate() + shape.getWidth() > factoryX + factory.getWidth()
				|| position.getyCoordinate() + shape.getHeight() > factoryY + factory.getHeight()) {
			return false;
		}

		return !factory.hasObstacleAt(shape) && !factory.hasMobileComponentAt(shape, this);
	}
	
	private int moveToNextPathPosition() {
		int displacement = 0;
		final Motion motion = computeMotion();
		if (motion != null) {
			//this.loginfo(getName()+"try to move");
			getFactory().lockGrid[motion.getTargetPosition().getxCoordinate()/speed][motion.getTargetPosition().getyCoordinate()/speed].lock();
			Position lastPosition = motion.getCurrentPosition();
			displacement = motion.moveToTarget();
			notifyObservers();
			try{getFactory().lockGrid[lastPosition.getxCoordinate()/speed][lastPosition.getyCoordinate()/speed].unlock();}
			catch(IllegalMonitorStateException ex) {}
		}
		else {
			//this.loginfo(getName()+"is blocked by an other robot");
			while (isLivelyLocked()) {
				final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
				
				if (freeNeighbouringPosition != null) {
					blockedTargetPosition = freeNeighbouringPosition;
					displacement = moveToNextPathPosition();
					computePathToCurrentTargetComponent();
				}
			}
		}
		return displacement;
	}
	
	private void computePathToCurrentTargetComponent() {
		final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
		LOGGER.info("Robot '" + getName() + "' computed path to '" + currTargetComponent.getName()
				+ "': " + currentPathPositions.size() + " step(s)");
	}
	
	private Motion computeMotion() {
		if (!currentPathPositionsIter.hasNext()) {

			// There is no free path to the target
			blocked = true;
			
			return null;
		}
		
		
		final Position targetPosition = getTargetPosition();
		final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
														   targetPosition.getyCoordinate(),
				   										   2,
				   										   2);
		
		// If there is another robot, memorize the blocked target position for the next run
		if (getFactory().hasMobileComponentAt(shape, this)) {
			this.blockedTargetPosition = targetPosition;
			
			return null;
		}

		// Reset the memorized position
		this.blockedTargetPosition = null;
			
		return new Motion(getPosition(), targetPosition);
	}
	
	private Position getTargetPosition() {
		// If a target position was memorized, it means that the robot was blocked during the last iteration 
		// so it waited for another robot to pass. So try to move to this memorized position otherwise move to  
		// the next position from the path
		return this.blockedTargetPosition == null ? currentPathPositionsIter.next() : this.blockedTargetPosition;
	}
	
	@JsonIgnore
	public boolean isLivelyLocked() {
		 final Position blockedTargetPosition = getBlockedTargetPosition();
		 if (blockedTargetPosition == null || getFactory() == null || getPosition() == null) {
			 return false;
		 }
		 final Component otherRobot =
				 getFactory().getMobileComponentAt(blockedTargetPosition, this);
		 return otherRobot != null &&
				 getPosition().equals(((Robot) otherRobot).getBlockedTargetPosition());
		}


	private boolean hasReachedCurrentTarget() {
		return currTargetComponent != null && overlays(currTargetComponent);
	}
	
	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}
	
	@Override
	@JsonIgnore
	public Style getStyle() {
		return blocked ? BLOCKED_STYLE : STYLE;
	}
}
