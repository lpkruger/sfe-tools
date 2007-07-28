/*
 * Created on Sep 16, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.sfdl;

import java.util.*;
/**
 * @author lpkruger
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class MapSet<K,V> {

	Map<K,Set<V>> map;
	Set<V> tmpl;

	public MapSet() {
		this(new HashMap<K,Set<V>>(), new TreeSet<V>());
	}
	
	public MapSet(Map<K,Set<V>> map) {
		this(map, new TreeSet<V>());
	}
	
	public MapSet(Map<K,Set<V>> map, Set<V> set) {
		this.map = map;
		this.tmpl = set;
	}

	public void add(K key, V value) {
		Set<V> l = map.get(key);
		if (l == null) {
			try {
				l = (Set<V>) tmpl.getClass().newInstance();
				map.put(key, l);
			} catch (Exception ex) {
				throw new AssertionError("can't instantiate new list");
			}
		}
		l.add(value);
	}

	public Set<V> get(K key) {
		return map.get(key);
	}

	public Set<K> keySet() {
		return map.keySet();
	}
}
