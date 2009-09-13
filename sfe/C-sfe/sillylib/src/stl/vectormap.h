/*
 * vectormap.h
 *
 *  Created on: Sep 12, 2009
 *      Author: louis
 */

#ifndef VECTORMAP_H_
#define VECTORMAP_H_

template <class K, class T>
class vector_map : private vector<std::pair<K, T> > {
	typedef std::pair<K, T> pair_t;
	typedef vector<pair_t> super;

	pair_t* get(K x) {
		return &super::operator[](x);
	}
	size_t v_size() {
		return super::size();
	}
	// invariant: vector size is 2+max_key
	// -1 is unused
	// -2 is end sentinel
public:
	void clear() {
		super::clear();
		this->resize(1, std::make_pair(-2, T()));
	}
	vector_map() {
		clear();
	}

	class iterator {
		pair_t *pair;
	public:
		iterator(pair_t *p0) : pair(p0) {}
		bool operator==(const iterator &b) const {
			return pair == b.pair;
		}
		bool operator!=(const iterator &b) const {
			return pair != b.pair;
		}
		iterator& operator++() {
			do {
				++pair;
			} while (pair->first == -1);
			return *this;
		}
		pair_t& operator*() {
			return (*pair);
		}
		pair_t* operator->() {
			return pair;
		}
	};
	T& operator[](K key) {
//		printf("size: %lu\n", v_size());
//		if (key<0)
//			throw out_of_range("key<0");
		if (size_t(key) >= v_size()-1) {
			get(v_size()-1)->first = -1;
			super::resize(key + 2, std::make_pair(-1, T()));
			get(key+1)->first = -2;
		}
		pair_t* t = get(key);
		t->first = key;
		return t->second;
	}
	iterator find(K key) {
		if (size_t(key) >= v_size())
			return end();
		pair_t* t = get(key);
		if (t->first == key)
			return iterator(t);
		else
			return end();

	}
	iterator begin() {
		iterator it(get(0)-1);
		return ++it;
	}
	iterator end() {
		return iterator(get(v_size()-1));
	}

};


#endif /* VECTORMAP_H_ */
