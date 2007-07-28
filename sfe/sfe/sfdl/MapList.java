/*
 * Created on Jun 5, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sfe.sfdl;

import java.util.*;

/**
 * @author lpkruger
 *
 * generic map of keys to lists
 */
public class MapList<K,V> {
	Map<K, List<V>> map;
	List<V> tmpl;

	public MapList() {
		this(new HashMap<K,List<V>>(), new LinkedList<V>());
	}

	public MapList(Map<K, List<V>> map) {
		this(map, new LinkedList<V>());
	}

	public MapList(Map<K, List<V>> map, List<V> list) {
		this.map = map;
		this.tmpl = list;
	}

	public void add(K key, V value) {
		List<V> l = map.get(key);
		if (l == null) {
			try {
				l = (List<V>) tmpl.getClass().newInstance();
				map.put(key, l);
			} catch (Exception ex) {
				throw new AssertionError("can't instantiate new list");
			}
		}
		l.add(value);
	}

	public List<V> get(K key) {
		return map.get(key);
	}

	public Set<K> keySet() {
		return map.keySet();
	}
}
