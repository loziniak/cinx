package pl.robotix.cinx;

import java.util.ArrayList;

import javafx.collections.ObservableListBase;

public class ObservableArrayList<E> extends ObservableListBase<E> {
	
	private ArrayList<E> list = new ArrayList<>();
	
	@Override
	public boolean add(E element) {
		boolean res;
		beginChange();
		res = list.add(element);
		nextAdd(list.size() - 1, list.size());
		endChange();
		return res;
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
