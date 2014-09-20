package common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class Bijection implements Serializable {
	private static final long serialVersionUID = 6526310928919842268L;
	// hy: name, posterior index
	final private HashMap<String, Integer> keys;
	// hy: posterior index, name
	final private Vector<String> values;
	private int size;

	public int getSize() {
		return size;
	}

	public boolean contains(String key) {
		return keys.containsKey(key);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		Iterator<String> v = values.iterator();
		int i = 0;
		while (v.hasNext()) {
			String s = v.next();
			sb.append(s);
			sb.append("=");
			sb.append(i++);
			if (v.hasNext())
				sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	// FIXME: should return an inmutable collection
	public Collection<Integer> values() {
		return keys.values(); // return posterior index
	}

	// FIXME: should return an inmutable collection
	public Collection<String> keys() {
		return keys.keySet();
	}

	public Bijection() {
		keys = new HashMap<String, Integer>();
		values = new Vector<String>();
		size = 0;
	}

	// hy:
	public Bijection(Bijection b) {
		size = 0;
		keys = new HashMap<String, Integer>();
		values = new Vector<String>();
		for (int i = 0; i < b.getSize(); i++) {
			this.put(b.get(i));
		}
	}

	public Bijection(String[] keys) {
		this();
		for (String k : keys)
			this.put(k);

	}

	public Integer get(String key) {
		return keys.get(key);
	}

	public String get(Integer value) {
		return values.get(value);
	}

	public Integer put(String key) {
		Integer value = keys.get(key);
		if (value == null) {
			value = size++; // hy: [TODO] starts from 0
			keys.put(key, value);// hy: yun,0
			values.add(value, key);// hy: 0,yun
		}
		return value;// hy: 0
	}

	static final Integer max(Collection<Integer> coll) {
		if (coll.isEmpty())
			return new Integer(-1);
		else
			return Collections.max(coll);
	}

}
