/*
 * Created on Mar 23, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.util.datastructure;

/**
 * @author alex
 *
 * An implementation of a MAP that maps keys to a SET of values
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapSet {
	private ConcurrentHashMap<Object, Set<Object>> map;

	public HashMapSet() {
		map = new ConcurrentHashMap<Object, Set<Object>>();
	}

	public HashMapSet(int initialCapacity) {
		map = new ConcurrentHashMap<Object, Set<Object>>(initialCapacity);
	}

	public void put(Object key, Object value) {
		if (!map.containsKey(key)) {
			map.put(key, Collections.synchronizedSet(new HashSet<Object>()));
		}
		Set<Object> set = map.get(key);
		synchronized (set) {
			set.add(value);
		}
	}

	public Set<Object> getSet(Object key) {
		return map.containsKey(key) ? map.get(key) : new HashSet<Object>();
	}

	/**
	 * Thread-safe verison of getSet()
	 * 
	 * @param key
	 * @return
	 */
	public Set<Object> getClonedSet(Object key) {
		return map.containsKey(key) ? new HashSet<Object>(map.get(key)) : new HashSet<Object>();
	}

	/**
	 * 
	 * @param key
	 * @param value
	 * @return true if element existed and is removed
	 */
	public boolean remove(Object key, Object value) {
		boolean result = false;

		if (map.containsKey(key)) {
			Set<Object> set = map.get(key);
			synchronized (set) {
				result = set.remove(value);
				if (set.isEmpty()) {
					map.remove(key);
					set = null;
				}
			}
		}

		return result;
	}

	public void removeAll(Object key) {
		map.remove(key);
	}

	public Set<Object> keySet() {
		return map.keySet();
	}

	public void clear() {
		HashSet<Object> keySet = new HashSet<Object>(map.keySet());
		for (Iterator<Object> i = keySet.iterator(); i.hasNext();) {
			map.remove(i.next());
		}
		map.clear();
		keySet.clear();
		keySet = null;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public String toString() {
		String output = "";
		HashSet<Object> keySet;
		synchronized (map) {
			keySet = new HashSet<Object>(map.keySet());
		}
		for (Object key : keySet) {
			Set<Object> set = map.get(key);
			synchronized (set) {
				output += "[" + key.toString() + ":" + set.toString() + "];";
			}
		}
		keySet.clear();
		keySet = null;

		return output;
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}
}
