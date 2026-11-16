package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Room extends Component {
	
	private static final long serialVersionUID = 1449569724908316962L;

	public static enum WALL {LEFT, TOP, RIGHT, BOTTOM};
	
	private static final int WALL_THICKNESS = 5;
	
	private final List<Area> areas;

	@JsonManagedReference("room-doors")
	private final List<Door> doors;

	public Room(final Factory factory,
				final RectangularShape shape,
				final String name) {
		super(factory, shape, name);

		areas = new ArrayList<>();
		doors = new ArrayList<>();
	}
	public Room() {
		this(null, null, null);
	}

	protected boolean addArea(final Area area) {
		return areas.add(area);
	}
	
	protected boolean addDoor(final Door door) {
		return doors.add(door);
	}
	
	public List<Area> getAreas() {
		return areas;
	}

	public List<Door> getDoors() {
		return doors;
	}

	@JsonIgnore
	public PositionedShape getLeftWall() {
		return getPositionedShape() == null ? null
				: new RectangularShape(getxCoordinate(), getyCoordinate(), WALL_THICKNESS, getHeight() + WALL_THICKNESS);
	}

	@JsonIgnore
	public PositionedShape getRightWall() {
		return getPositionedShape() == null ? null
				: new RectangularShape(getxCoordinate() + getWidth(), getyCoordinate(), WALL_THICKNESS, getHeight() + WALL_THICKNESS);
	}

	@JsonIgnore
	public PositionedShape getTopWall() {
		return getPositionedShape() == null ? null
				: new RectangularShape(getxCoordinate(), getyCoordinate(), getWidth(), WALL_THICKNESS);
	}

	@JsonIgnore
	public PositionedShape getBottomWall() {
		return getPositionedShape() == null ? null
				: new RectangularShape(getxCoordinate(), getyCoordinate() + getHeight(), getWidth(), WALL_THICKNESS);
	}

	@Override
	public boolean overlays(final PositionedShape shape) {
		return getPositionedShape() != null && shape != null
				&& (getLeftWall().overlays(shape) || getRightWall().overlays(shape) ||
			   getTopWall().overlays(shape) || getBottomWall().overlays(shape));
	}

	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		if (shape == null || getPositionedShape() == null) {
			return false;
		}

		final Door overlayedDoor = getOverlayedDoor(shape);
		
		if (overlayedDoor != null) {
			return overlayedDoor.canBeOverlayed(shape);
		}
		
		if (getLeftWall().overlays(shape) || getRightWall().overlays(shape) ||
			getTopWall().overlays(shape) || getBottomWall().overlays(shape)) {
			return false;
		}
		
		return true;
	}
	
	private Door getOverlayedDoor(final PositionedShape shape) {
		for (final Door door : getDoors()) {
			if (door.overlays(shape)) {
				return door;
			}
		}
		
		return null;
	}

	@Override
	public String toString() {
		return super.toString() + " areas=" + areas + "]";
	}
}
