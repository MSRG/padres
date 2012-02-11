package ca.utoronto.msrg.padres.common.util.datastructure;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use this class if you want to use the toString() of your object to uniquely
 * identify your object
 * 
 * @author cheung
 * 
 */

public class StringIndexedHashSet<E> {

	// data structure that stores the set objected indexed by string key
	private final ConcurrentHashMap<String, E> map;

	/**
	 * Constructor
	 */
	public StringIndexedHashSet() {
		map = new ConcurrentHashMap<String, E>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(E obj) {
		return map.put(obj.toString(), obj) == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Object obj) {
		return map.remove(obj.toString()) != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#isEmpty()
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#clear()
	 */
	public void clear() {
		map.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	public boolean contains(Object obj) {
		return map.containsKey(obj.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#size()
	 */
	public int size() {
		return map.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#iterator()
	 */
	public Iterator<E> iterator() {
		return map.values().iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return map.values().toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#toArray(T[])
	 * 
	 * WARNING, implementation not tested.
	 */
	public E[] toArray(E[] array) {
		return map.values().toArray(array);
	}

	public Object[] toArray() {
		Object[] array = new Object[map.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = map.get(i);
		}

		return array;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<? extends Object> c) {
		for (Object obj : c) {
			if (!map.keySet().contains(obj.toString()))
				return false;
		}
		return true;
	}

	public boolean addAll(Collection<E> c) {
		boolean added = false;
		for (E obj : c) {
			if (map.put(obj.toString(), obj) == null) {
				added |= true;
			}
		}
		return added;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#retainAll(java.util.Collection)
	 * 
	 * TODO:
	 */
	public boolean retainAll(Collection<E> c) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<? extends Object> c) {
		boolean changed = false;
		for (Object obj : c) {
			if (map.keySet().contains(obj.toString())) {
				map.remove(obj.toString());
				changed |= true;
			}
		}

		return changed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Set#removeAll(StringIndexedHashSet)
	 */
	public <T> boolean removeAll(StringIndexedHashSet<T> c) {
		boolean changed = false;
		for (Iterator<T> i = c.iterator(); i.hasNext();) {
			T obj = i.next();
			if (map.keySet().contains(obj.toString())) {
				map.remove(obj.toString());
				changed |= true;
			}
		}
		return changed;
	}
}
