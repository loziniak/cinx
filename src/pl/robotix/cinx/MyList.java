package pl.robotix.cinx;

import java.util.ArrayList;

import javafx.collections.ObservableListBase;

public class MyList<E> extends ObservableListBase<E> {
	
	private ArrayList<E> list = new ArrayList<>();
	
	public boolean add(E element) {
		return list.add(element);
	}
	
	@Override
	public E get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}

}
