package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;

public class LocalFactoryModelChangedNotifier implements FactoryModelChangedNotifier{
	@JsonIgnore
	private transient List<Observer> observers;
	
	public LocalFactoryModelChangedNotifier() {
		observers = null;
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
	
	@Override
	public void notifyObservers() {
		for (final Observer observer : getObservers()) {
			observer.modelChanged();
		}
	}
}
