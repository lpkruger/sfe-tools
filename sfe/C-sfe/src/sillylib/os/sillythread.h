/*
 * sillythread.h
 *
 *  Created on: Aug 28, 2009
 *      Author: louis
 */

#include <pthread.h>
#include "errno.h"

#ifndef SILLYTHREAD_H_
#define SILLYTHREAD_H_

#if 1
static inline void sillythread_dummy(...) {}
#define DF sillythread_dummy
#else
#define DF debug_printf
//#ifndef SILLYDEBUG_H_
void debug_printf(const char *fmt, ...)
	__attribute__ ((format (printf, 1, 2)));
//#endif
#endif

namespace silly {
namespace thread {

struct ThreadException : public silly::ErrnoException {
	typedef ErrnoException super;
	ThreadException(int err) : super (err) {}
};

static inline void throwIfError(int code) {
	if (!code)
		return;
	throw ThreadException(code);
}

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
		throwIfError(pthread_cancel(thread));
	}

	void detach() {
		throwIfError(pthread_detach(thread));
		thread = -1;
	}
	void* join() {
		if (thread==pthread_t(-1)) {
			throw ThreadException(ESRCH);
			// ESRCH?
			// ESTALE?
			// EINVAL?
		}
		void *retval;
		throwIfError(pthread_join(thread, &retval));
		thread = -1;
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
	PrimitiveMutex() {
		// TODO: PTHREAD_MUTEX_RECURSIVE
		memset(&mux, 0, sizeof(mux));
		throwIfError(pthread_mutex_init(&mux, NULL));

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

public:
	void wait() {
		throwIfError(pthread_cond_wait(&cond, &mux));
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

	NOCOPY(Lock)

protected:
	Lock() : mux(NULL), locked(false) {}

public:
	Lock(Mutex *mux0) : mux(mux0), locked(false) {
		mux->acquire();
		locked = true;
	}
	Lock(Mutex &mux0) : mux(&mux0), locked(false) {
		mux->acquire();
		locked = true;
	}
	~Lock() {
		if (!locked)
			throw ThreadException(EINVAL);
		locked = false;
		mux->release();
	}
#if 0
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
#endif
} RESOURCE;


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
	static void* go(Thread *that) {
		void *retval = that->run();
		bool deletethat = false;
		{
			Lock lock(that->threadLock);
			that->returned = true;
			that->retval = retval;
			that->running = false;
			if (that->deleteOnTerm)
				deletethat = true;
			else
				that->threadLock.notifyAll();
		}
		if (deletethat)
			delete that;
		return retval;
	}
public:
	Thread(Runnable *f0 = NULL) : super() {
		func = f0;
		running = false;
		returned = false;
		deleteOnJoin = false;
		deleteOnTerm = false;
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
				threadLock.wait();
			if (deleteOnJoin)
				delete this;
			return retval;
		}
	}
};

// this would normally be a static variable
class ThreadLocal {
	pthread_key_t key;

public:
	typedef void (*destructor_function)(void*);
	ThreadLocal() : key(0) {
		 throwIfError(pthread_key_create(&key, NULL));
	}
	ThreadLocal(destructor_function func) : key(0) {
		throwIfError(pthread_key_create(&key, func));
	}
	~ThreadLocal() {
		throwIfError(pthread_key_delete(key));
	}

	void *get() const {
		return pthread_getspecific(key);
	}
	void set(void *value) const {
		throwIfError(pthread_setspecific(key, value));
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
	vector<Thread*> threads;
	queue<Runnable*> workqueue;
	bool stopping;

	struct WorkThread : public Thread {
		ThreadPool *pool;
		WorkThread(ThreadPool *p0) : pool(p0) {
			deleteOnTerm = true;
			DF("worker thread %08x created", (uint)this);
		}
		~WorkThread() {
			DF("worker thread %08x deleted", (uint)this);
		}
	public:
		void* run() {
			DF("worker thread %08x started", (uint)this);
			Runnable *job;
			do {
				job = NULL;
				{
					Lock lock(pool->pool_mux);
					if (!pool->workqueue.empty()) {
						job = pool->workqueue.front();
						pool->workqueue.pop();
					}
				}
				if (job) {
					DF("worker thread %08x start job %08x", (uint)this, (uint)job);
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
				DF("worker thread %08x stopped", (uint)this);
				pool->pool_mux.notifyAll();
			}

			return NULL;
		}
	};
public:
	ThreadPool(uint n) : max(n), stopping(false) {
		threads.reserve(n);
	}
	void submit(Runnable *job) {
		Lock lock(pool_mux);
		DF("job submission received %08x", (uint)job);
		if (stopping) {
			throw ThreadException(ECANCELED);
		}
		workqueue.push(job);
		if (threads.size() < max) {
			WorkThread *th = new WorkThread(this);
			threads.push_back(th);
			th->start();
		}
	}
	void stop() {
		DF("Stopping pool");
		Lock lock(pool_mux);
		stopping = true;
	}
	void waitIdle() {
		DF("Waiting for idle");
		Lock lock(pool_mux);
		while (!threads.empty())
			pool_mux.wait();
	}
	void stopWait() {
		stop();
		waitIdle();
	}
};

}
}


#endif /* SILLYTHREAD_H_ */
