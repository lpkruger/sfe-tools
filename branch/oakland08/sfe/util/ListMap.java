package sfe.util;

import java.util.*;

// ordered map
public class ListMap<K,V> implements Map<K,V> {
	Map<K, V> map = new HashMap<K,V>();
	List<K> list = new ArrayList<K>();
	
	static class Ent<K,V> implements Map.Entry<K, V> {
		K key;
		V val;
		Ent(K k, V v) {
			this.key = k;
			this.val = v;
		}
		public K getKey() {
			return key;
		}
		public V getValue() {
			return val;
		}
		public V setValue(V value) {
			return this.val = value;
		}
	}
	
	public void clear() {
		map.clear();
		list.clear();
	}
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}
	public Set<Map.Entry<K, V>> entrySet() {
		ListSet<Map.Entry<K, V>> ls = new ListSet<Map.Entry<K, V>>();
		for (K key : list) {
			ls.add(new Ent<K,V>(key, map.get(key)));
		}
		return ls;
	}
	public V get(Object key) {
		return map.get(key);
	}
	public boolean isEmpty() {
		return map.isEmpty();
	}
	public Set<K> keySet() {
		return new ListSet<K>(list);
	}
	public V put(K key, V value) {
		if (!map.containsKey(key)) {
			list.add(key);
		}
		return map.put(key, value);
	}
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry ent : m.entrySet()) {
			put((K)ent.getKey(), (V)ent.getValue());	
		}
	}
	public V remove(Object key) {
		list.remove(key);
		return map.remove(key);
	}
	public int size() {
		return map.size();
	}
	public Collection<V> values() {
		ArrayList<V> vv = new ArrayList<V>();
		for (K key : list) {
			vv.add(map.get(key));
		}
		return vv;
	}
	
	int indexOfKey(K key) {
		return list.indexOf(key);
	}
}
