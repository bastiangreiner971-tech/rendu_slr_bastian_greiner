package fr.tp.inf112.projects.robotsim.model.shapes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.tp.inf112.projects.canvas.model.Point;
import fr.tp.inf112.projects.canvas.model.PolygonShape;
import fr.tp.inf112.projects.canvas.model.impl.BasicPoint;

public class BasicPolygonShape extends PositionedShape implements PolygonShape {
	
	private static final long serialVersionUID = -1764316101910546849L;

	private final Set<Point> points;
	
	public BasicPolygonShape() {
		super(0, 0);
		
		this.points = new LinkedHashSet<>();
	}
	
	@Override
	@JsonIgnore
	public Set<Point> getPoints() {
		return points;
	}

	@JsonProperty("points")
	public List<PolygonPoint> getSerializedPoints() {
		final List<PolygonPoint> serializedPoints = new ArrayList<>();
		for (final Point point : points) {
			serializedPoints.add(new PolygonPoint(point.getxCoordinate(), point.getyCoordinate()));
		}

		return serializedPoints;
	}

	@JsonProperty("points")
	public void setSerializedPoints(final List<PolygonPoint> serializedPoints) {
		points.clear();
		if (serializedPoints != null) {
			for (final PolygonPoint point : serializedPoints) {
				points.add(new BasicPoint(point.getXCoordinate(), point.getYCoordinate()));
			}
		}
		if (!points.isEmpty()) {
			updatePosition();
		}
	}
	
	public boolean addPoint(final Point point) {
		final boolean added = getPoints().add(point);
		
		updatePosition();
		
		return added;
	}

	public static class PolygonPoint {
		private int xCoordinate;
		private int yCoordinate;

		public PolygonPoint() {
		}

		public PolygonPoint(final int xCoordinate, final int yCoordinate) {
			this.xCoordinate = xCoordinate;
			this.yCoordinate = yCoordinate;
		}

		public int getXCoordinate() {
			return xCoordinate;
		}

		public void setXCoordinate(final int xCoordinate) {
			this.xCoordinate = xCoordinate;
		}

		public int getYCoordinate() {
			return yCoordinate;
		}

		public void setYCoordinate(final int yCoordinate) {
			this.yCoordinate = yCoordinate;
		}
	}
	
	private void updatePosition() {
		int minxCoordinate = Integer.MAX_VALUE;
		int minyCoordinate = minxCoordinate;
		
		for (final Point point : getPoints()) {
			minxCoordinate = Math.min(minxCoordinate, point.getxCoordinate());
			minyCoordinate = Math.min(minyCoordinate, point.getyCoordinate());
		}
		
		setxCoordinate(minxCoordinate);
		setyCoordinate(minyCoordinate);
	}
	
	@Override
	@JsonIgnore
	public int getWidth() {
		if (getPoints().isEmpty()) {
			return 0;
		}

		int minCoordinate = Integer.MAX_VALUE;
		int maxCoordinate = 0;
		
		for (final Point point : getPoints()) {
			final int coordinate = point.getxCoordinate();

			minCoordinate = Math.min(minCoordinate, coordinate);
			maxCoordinate = Math.max(maxCoordinate, coordinate);
		}
		
		return maxCoordinate - minCoordinate;
	}

	@Override
	@JsonIgnore
	public int getHeight() {
		if (getPoints().isEmpty()) {
			return 0;
		}

		int minCoordinate = Integer.MAX_VALUE;
		int maxCoordinate = 0;
		
		for (final Point point : getPoints()) {
			final int coordinate = point.getyCoordinate();

			minCoordinate = Math.min(minCoordinate, coordinate);
			maxCoordinate = Math.max(maxCoordinate, coordinate);
		}
		
		return maxCoordinate - minCoordinate;
	}
}
