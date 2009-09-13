/*
 * counter.h
 *
 *  Created on: Sep 2, 2009
 *      Author: louis
 */

#define NOCOUNT 1

#ifndef SILLYMEM_COUNTER_H_
#define SILLYMEM_COUNTER_H_

namespace silly {
namespace mem {

#ifndef NOCOUNT
class copy_counter {
private:
	const char *name;
	int copy_count;
	int move_count;
	int eq_count;
	int refeq_count;
public:
	copy_counter(const char *n) : name(n) {}
	virtual void print_counter(FILE *fh = stderr) {
		fprintf(fh, "%s:  con& %d   con&& %d\n=& %d   =&& %d\n",
				name, copy_count, move_count, eq_count, refeq_count);
	}
	virtual ~copy_counter() {}

	void count_copy() { ++copy_count; }
	void count_move() { ++move_count; }
	void count_eq() { ++eq_count; }
	void count_refeq() { ++refeq_count; }
	void uncount_eq() { --eq_count; }  // because sometimes ctors call
	void uncount_refeq() { --refeq_count; } // operator=
};

class object_counter : public copy_counter {
	int con_count;
	int des_count;
public:
	object_counter(const char *n) : copy_counter(n) {}
	void print_counter(FILE *fh = stderr) {
		copy_counter::print_counter(fh);
		fprintf(stderr, "con %d   des %d\n",
				con_count, des_count);
	}
	void count_con() { ++con_count; }
	void count_des() { ++des_count; }
};
template<class C>
struct count_printer {
	copy_counter *obj;
	 count_printer() {
		obj = &C::counter;
	}
	~count_printer() {
		obj->print_counter();
//		byte_buf::counter.print_counter();
//		CipherKey::counter.print_counter();
	}
} __attribute__ ((unused));
#else
struct copy_counter {
	copy_counter(const char *n) {}
	void count_copy() {}
	void count_move() {}
	void count_eq() {}
	void count_refeq() {}
	void uncount_eq() {}    // because sometimes ctors call
	void uncount_refeq() {} // operator=
};
struct object_counter : public copy_counter {
	object_counter(const char *n) : copy_counter(n) {}
	void count_con() {}
	void count_des() {}
	void print_counter(FILE *fh = stderr) {}
};
template<class C>
struct count_printer {
} __attribute__ ((unused));

#endif


#if USE_RVALREFS
/* print_backtrace(5, #type); */
#define COPY_COUNTER(type) \
		type(const type &copy) { counter.count_copy(); } \
		type(type &&move) { counter.count_move(); } \
		type& operator=(const type &copy) { \
			counter.count_eq(); \
			return *this; } \
		type& operator=(type &&copy) { \
			counter.count_refeq(); \
			return *this; }

#define COPY_COUNTER_DERIVED(type, super) \
		type(const type &copy, super) : super(copy) { counter.count_copy();  } \
		type(type &&move) : super(move) { counter.count_move(); } \
		type& operator=(const type &copy) { \
			super::operator=(copy); \
			counter.count_eq(); \
			return *this; } \
		type& operator=(type &&copy) { \
			super::operator=(copy); \
			counter.count_refeq(); \
			return *this; }
#else
#define COPY_COUNTER(type) \
		type(const type &copy) { counter.count_copy(); } \
		type& operator=(const type &copy) { \
			counter.count_eq(); \
			return *this; }

#define COPY_COUNTER_DERIVED(type, super) \
		type(const type &copy, super) : super(copy) { counter.count_copy();  } \
		type& operator=(const type &copy) { \
			super::operator=(copy); \
			counter.count_eq(); \
			return *this; }

#endif


}
}

#endif /* COUNTER_H_ */
