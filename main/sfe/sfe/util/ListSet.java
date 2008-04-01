package sfe.util;

import java.util.*;

public class ListSet<V> implements Set<V> {
	List<V> list;
	public boolean add(V e) {
		if (list.contains(e))
			return false;
		list.add(e);
		return true;
	}
	
	ListSet() {
		this.list = new ArrayList<V>();
	}
	ListSet(List<V> list) {
		this.list = list;
	}

	public boolean addAll(Collection<? extends V> c) {
		boolean changed = false;
		for (V ent : c) {
			changed |= add(ent);
		}
		return changed;
	}

	public void clear() {
		list.clear();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator<V> iterator() {
		return list.iterator();
	}

	public boolean remove(Object o) {
		return list.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public int size() {
		return list.size();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

}
