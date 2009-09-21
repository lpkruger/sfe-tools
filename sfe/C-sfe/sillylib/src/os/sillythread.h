/*
 * sillythread.h
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */


#ifndef SILLYTHREAD_H_
#define SILLYTHREAD_H_

#include <pthread.h>
#include <time.h>
#include <sys/time.h>
#include <sys/resource.h>
#include "errno.h"

//TODO: this is stupid
#include <sys/syscall.h>
#define clock_gettime(X,Y)  syscall(__NR_clock_gettime, X, Y)

#if !defined(DF)
#define SILLYTHREAD_DEFINED_DF
#if 1
static inline void sillythread_dummy(...) {}
#define DF sillythread_dummy
#else
#define DF debug_printf
//#ifndef SILLYDEBUG_H_
void debug_printf(const char *fmt, ...)
	__attribute__ ((format (printf, 1, 2)));
#endif
#endif
///tmp vvv
//void debug_printf(const char *fmt, ...)
//	__attribute__ ((format (printf, 1, 2)));
///tmp ^^^

namespace silly {
namespace thread {

struct ThreadException : public silly::ErrnoException {
	typedef ErrnoException super;
	ThreadException(int err) : super (err) {}
};

static inline void throwIfError0(int code) {
	if (!code)
		return;
	throw ThreadException(code);
}
#define throwIfError(condition) do {		\
	int err = condition;					\
	if (err)								\
		fprintf(stderr, "%s = %d at %s:%d\n", #condition, err, __FILE__, __LINE__);	\
	throwIfError0(err);	} while(0)

class PrimitiveThread {
	NOCOPY(PrimitiveThread)
protected:
	pthread_t thread;

public:
	typedef void* (*start_function)(void *arg);

	PrimitiveThread() : thread(-1) {}
	PrimitiveThread(start_function run, void* arg=NULL) : thread(-1) {
		start(run, arg);
	}

	void start(start_function run, void* arg=NULL) {
		start(1, 500, run, arg);
	}
	void start(int tries, start_function run, void* arg=NULL) {
		start(tries, 500, run, arg);
	}
	void start(uint tries, int usec, start_function run, void* arg=NULL) {
		uint count=0;
		while (count < tries)
			try {
				throwIfError(pthread_create(&thread, NULL, run, arg));
				return;
			} catch (ThreadException ex) {
				thread = -1;
				if (ex.getErrno()==EAGAIN && ++count < tries) {
					usleep(usec);
					continue;
				}
				throw;
			}
	}

	void stop() {
		if (thread==pthread_t(-1))
			throw ThreadException(ESRCH);
		throwIfError(pthread_cancel(thread));
	}

	void detach() {
		if (thread==pthread_t(-1))
			throw ThreadException(ESRCH);
		throwIfError(pthread_detach(thread));
	}
	void* join() {
		if (thread==pthread_t(-1))
			throw ThreadException(ESRCH);
		void *retval;
		throwIfError(pthread_join(thread, &retval));
		return retval;
	}
} RESOURCE;


class ConditionVar {
	NOCOPY(ConditionVar)
protected:
	pthread_cond_t cond;

public:
	ConditionVar() {
		memset(&cond, 0, sizeof(cond));
		throwIfError(pthread_cond_init(&cond, NULL));
	}
	~ConditionVar() {
		throwIfError(pthread_cond_destroy(&cond));
	}

	void notify() {
		throwIfError(pthread_cond_signal(&cond));
	}
	void notifyAll() {
		throwIfError(pthread_cond_broadcast(&cond));
	}
};

class PrimitiveMutex {
	NOCOPY(PrimitiveMutex)
protected:
	pthread_mutex_t mux;

public:
	PrimitiveMutex(int kind=PTHREAD_MUTEX_NORMAL) {
		pthread_mutexattr_t attr;
		pthread_mutexattr_init(&attr);
		pthread_mutexattr_settype(&attr, kind);
		memset(&mux, 0, sizeof(mux));
		throwIfError(pthread_mutex_init(&mux, &attr));
	}

	~PrimitiveMutex() {
		throwIfError(pthread_mutex_destroy(&mux));
	}

public:
	void acquire() {
		throwIfError(pthread_mutex_lock(&mux));
	}

	void release() {
		throwIfError(pthread_mutex_unlock(&mux));
	}
};

class Mutex : protected PrimitiveMutex, protected ConditionVar {
	pthread_t owner;
public:
	Mutex() : owner(-1) {}
	~Mutex();

private:
	void wait() {
		throwIfError(pthread_cond_wait(&cond, &mux));
	}
	void wait(ulong milsec) {
		timespec timer;
		clockid_t clock;
		pthread_condattr_t attr;
		throwIfError(pthread_condattr_init(&attr));
		throwIfError(pthread_condattr_getclock(&attr, &clock));
		int err = clock_gettime(clock, &timer);
		if(err==-1)
			throwIfError(errno);
		timer.tv_nsec += (milsec%1000)*1000000;
		timer.tv_sec +=  milsec/1000 + (timer.tv_nsec/1000000000);
		timer.tv_nsec %= 1000000000;
		int err2 = pthread_cond_timedwait(&cond, &mux, &timer);
		if (err2 != ETIMEDOUT)
			throwIfError(err2);
	}
	void notify() {
		ConditionVar::notify();
	}

	void notifyAll() {
		ConditionVar::notifyAll();
	}
	friend class Lock;

};

class Lock {
	Mutex *mux;
	bool locked;
	bool threadhadlock;
	NOCOPY(Lock)
	//friend class Mutex;
protected:
	Lock() : mux(NULL), locked(false) {}

public:
	Lock(Mutex *mux0) : mux(mux0), locked(false), threadhadlock(false) {
		if (mux->owner == pthread_self()) {
			threadhadlock = true;
			locked = true;
		} else {
			lock();
		}
	}
	Lock(Mutex &mux0) : mux(&mux0), locked(false), threadhadlock(false) {
		if (mux->owner == pthread_self()) {
			threadhadlock = true;
			locked = true;
		} else {
			lock();
		}
	}
	~Lock() {
		if (locked && !threadhadlock) {
			unlock();
		} else if (!locked && threadhadlock) {
			lock();
		}
	}
	void lock() {
		if (!locked) {
			mux->acquire();
			mux->owner = pthread_self();
			locked = true;
			DF("acquire lock %x\n", mux->owner, &mux);
		}
	}
	void unlock() {
		if (locked) {
			DF("release lock %x\n", mux->owner, &mux);
			mux->owner = -1;
			mux->release();
			locked = false;
		}
	}
#if 1
	void notify() {
		if (!locked)
			throw new ThreadException(EINVAL);
		mux->notify();
	}
	void notifyAll() {
		if (!locked)
			throw new ThreadException(EINVAL);
		mux->notifyAll();
	}
	void wait() {
		if (!locked)
			throw new ThreadException(EINVAL);
		mux->wait();
	}
	void wait(ulong milsec) {
		if (!locked)
			throw new ThreadException(EINVAL);
		mux->wait(milsec);
	}
#endif
} RESOURCE;


inline Mutex::~Mutex() {
#if 0
	Lock lock(this);
	fprintf(stderr, "acquired lock before destruction\n");
	notifyAll();
	if (lock.threadhadlock)
		fprintf(stderr, "weird\n");
	// PrimitiveMutex destructor will destroy the object
#endif
}

///////////////////////
struct Runnable {
	virtual ~Runnable() {}
	virtual void* run() = 0;
};
struct FuncRunner : public Runnable {
	PrimitiveThread::start_function fn;
	void* arg;
	FuncRunner(PrimitiveThread::start_function fn0, void* arg0=NULL) {
		fn = fn0;
		arg = arg0;
	}
	void* run() {
		return fn(arg);
	}
};

class Thread : protected PrimitiveThread, public Runnable {
protected:
	typedef PrimitiveThread super;
	Mutex threadLock;
	Runnable *func;
	bool running;
	bool returned;
	bool deleteOnJoin;
	bool deleteOnTerm;
	void* retval;

	struct cleaner {
		Thread *that;
		void *retval;
		cleaner(Thread *t0) : that(t0), retval(NULL) {}
		~cleaner() {
			bool deletethat = false;

			Lock lock(that->threadLock);
			that->returned = true;
			that->retval = retval;
			that->running = false;
			if (that->deleteOnTerm)
				deletethat = true;
			else
				lock.notifyAll();

			lock.unlock();
			if (deletethat)
				delete that;
		}
	};
	static void* go(Thread *that) {

		cleaner it(that);
		it.retval = that->run();

//		try {
//		} catch (std::exception ex) {
//			fprintf(stderr, "exception caught %s", ex.what());
//		} catch (__cxxabiv1::__forced_unwind &w) {
//			throw;
//		} catch (...) {
//			fprintf(stderr, "unknown exception caught...");
//		}

		return it.retval;
	}
	~Thread() {}	// thread deletes itself
public:
	Thread(Runnable *f0 = NULL) : super() {
		func = f0;
		running = false;
		returned = false;
		deleteOnJoin = false;
		deleteOnTerm = true;
		retval = NULL;
	}
	void* run() {
		if (func)
			return func->run();
		return NULL;
	}

	void start(start_function run, void* arg=NULL) {
		func = new FuncRunner(run, arg);
		start();
	}
	void start(uint tries, int usec, start_function run, void* arg=NULL) {
		func = new FuncRunner(run, arg);
		start(tries, usec);
	}
	void start(uint tries = -1, int usec = 500) {
		super::start(tries, usec, (start_function) go, this);
		running = true;
		super::detach();
	}
	void stop() {
		Lock lock(threadLock);
		if (returned || !running)
			return;
		super::stop();
	}

	void* join() {
		if (returned)
			return retval;
		{
			Lock lock(threadLock);
			if (!running)
				throw ThreadException(EINVAL);
			while (!returned)
				lock.wait();
			if (deleteOnJoin) {
				deleteOnTerm = false;
				delete this;
			}
			return retval;
		}
	}
};

// this would normally be a static variable
class PrimitiveThreadLocal {
	pthread_key_t key;

public:
	typedef void (*destructor_function)(void*);
	PrimitiveThreadLocal() : key(0) {
		 throwIfError(pthread_key_create(&key, NULL));
	}
	PrimitiveThreadLocal(destructor_function func) : key(0) {
		throwIfError(pthread_key_create(&key, func));
	}
	~PrimitiveThreadLocal() {
		throwIfError(pthread_key_delete(key));
	}

	void *get() const {
		return pthread_getspecific(key);
	}
	void set(void *value) const {
		throwIfError(pthread_setspecific(key, value));
	}
};

template<class T>
class ThreadLocal : private PrimitiveThreadLocal {
	typedef PrimitiveThreadLocal super;

public:
	ThreadLocal() : super(&destroy) {}

	static void destroy(T *p) {
		delete p;
	}
	operator T*() {
		T* p = static_cast<T*>(get());
		if (!p) {
			set(new T());
		}
		return p;
	}
};


//#define SYNCHRONIZE(mutex) Lock mutex##_lock(&mutex)
}
}

#include <vector>
#include <queue>
#include <algorithm>
using ::std::vector;
using ::std::queue;


namespace silly {
namespace thread {

class ThreadPool {
	Mutex pool_mux;
	uint max;
	volatile int niceValue;
	vector<Thread*> threads;
	queue<Runnable*> workqueue;
	bool stopping;

	class MonitorThread;
	MonitorThread *monitor;

	struct WorkThread : public Thread {
		ThreadPool *pool;
		WorkThread(ThreadPool *p0) : pool(p0) {
			deleteOnTerm = true;
			DF("worker thread %08lx created", (ulong)this);
		}
		~WorkThread() {
			DF("worker thread %08lx deleted", (ulong)this);
		}
	public:
		void* run() {
			DF("worker thread %08lx started", (ulong)this);
			int mynice = pool->niceValue;
			if (mynice) {
				setpriority(PRIO_PROCESS, 0, mynice);
				DF("set workthread priority to %d\n", mynice);
			}
			Runnable *job;
			do {
				job = NULL;
				if (mynice != pool->niceValue) {
					DF("thread exit to reset priority\n");
					break;
				} else {
					Lock lock(pool->pool_mux);
					if (!pool->workqueue.empty()) {
						job = pool->workqueue.front();
						pool->workqueue.pop();
					}
				}
				if (job) {
					DF("worker thread %08lx start job %08lx", (ulong)this, (ulong)job);
					job->run();
				}
			} while (job);
			// shut down when nothing to do
			{
				Lock lock(pool->pool_mux);
				vector<Thread*>::iterator this_thread =
						std::find(pool->threads.begin(),
								pool->threads.end(), this);
				pool->threads.erase(this_thread);
				DF("worker thread %08lx stopped", (ulong)this);
				lock.notifyAll();
			}

			return NULL;
		}
	};
	class MonitorThread : public Thread {
		ThreadPool *pool;
	public:
		MonitorThread(ThreadPool *p) : pool(p) {}
		void* run() {

			Lock lock(pool->pool_mux);
			while (!pool->stopping || !pool->workqueue.empty()) {
				while (pool->threads.size() <
						std::min(pool->max, pool->workqueue.size())) {
					WorkThread *th = new WorkThread(pool);
					pool->threads.push_back(th);
					th->start();
					lock.notifyAll();
				}
				if (!pool->stopping || !pool->workqueue.empty())
					lock.wait();
			}
			pool->monitor = NULL;
			lock.notifyAll();
			return NULL;
		}
	};
public:
	ThreadPool(uint n, int nice=0) : max(n), niceValue(nice), stopping(false) {
		DF("ThreadPool size %d created\n", n);
		threads.reserve(n);
		monitor = new MonitorThread(this);
		monitor->start();
	}
	~ThreadPool() {
		Lock lock(pool_mux);
		while (monitor) {
			lock.notifyAll();
			lock.wait();
		}
	}

	void setPriority(int n) {
		niceValue = n;
	}
	void submit(Runnable *job) {
		Lock lock(pool_mux);

		DF("job submission received %08lx", (ulong)job);
		if (stopping) {
			throw ThreadException(ECANCELED);
		}
		workqueue.push(job);
		lock.notifyAll();
//		if (threads.size() < max) {
//			WorkThread *th = new WorkThread(this);
//			threads.push_back(th);
//			th->start();
//		}
	}
	void stop() {
		DF("Stopping pool");
		Lock lock(pool_mux);
		stopping = true;
		lock.notifyAll();
	}
	void waitIdle() {
		DF("Waiting for idle");
		Lock lock(pool_mux);
		while (!workqueue.empty() || !threads.empty())
			lock.wait();
	}
	void stopWait() {
		stop();
		waitIdle();
	}
};

#ifdef SILLYTHREAD_DEFINED_DF
#undef SILLYTHREAD_DEFINED_DF
#undef DF
#endif

}
}


#endif /* SILLYTHREAD_H_ */
