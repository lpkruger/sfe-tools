/*
 * AuthStreams.h
 *
 *  Created on: Sep 4, 2009
 *      Author: louis
 */

#define DROPBEAR

#ifndef AUTHSTREAMS_H_
#define AUTHSTREAMS_H_

//#define DEBUG 1
//#define DEBUG2 1
#include "sillyio.h"
#include "sillytype.h"
#include "../sillylib/os/sillythread.h"
#include <string>

#include "sillydebug.h"

using namespace silly::thread;
using namespace silly::io;
using namespace std;

// interface to ssh
extern bool ssh_writePacket(byte *buf, int length);

///////////
class AuthStreams;

//#define Dio(x) std::cerr << x << std::endl;
#define Dio DC
//#define Dio(s) do {} while(0);


inline void D_ON(const bit_vector &vv) {
	for (uint i=0; i<vv.size(); ++i) {
		printf(vv[i] ? "1" : "0");
	}
	printf("\n");
}
inline void D_ON(const map<int,bool> &vv) {
	map<int,bool>::const_iterator it;
	for (it=vv.begin(); it!=vv.end(); ++it) {
		printf("%d: %d\n", it->first, it->second);
	}
}

class AuthInputStream : public DataInput {
	typedef DataInput super;
public:
	AuthStreams *streams;

	AuthInputStream(AuthStreams *str) {
		Dio("Starting up....  vrooom....");
		streams = str;
	}

	virtual int tryRead(byte* c, int len);

};


class AuthOutputStream : public BytesDataOutput {
	typedef BytesDataOutput super;
public:
	AuthStreams *streams;
    boolean closed;

	AuthOutputStream(AuthStreams *str) : closed(false) {
		streams = str;
	}

	virtual int tryWrite(const byte* c, int len);
    virtual void close();
};

struct AuthStreams : public Runnable {

	Mutex mux;
	boolean failure_flag;
	boolean done_flag;

	//static const boolean useMD5 = true;
	//static const boolean use_R = true;
	static const boolean useMD5 = true;
	static const boolean use_R = false;
	static const boolean useSeperateSocket = false;
	static const int num_circuits_default = 2;

	// there will be 2 threads: One is Pure Java and executes the protocol,
	// the other is called from native code and goes back and forth
	AuthInputStream *authIn;
	AuthOutputStream *authOut;
	Thread *protocolThread;

	byte_buf outputbytes;
	byte_buf inputbytes;
	int inputbytesunread;
	boolean waitingForIO;
	//SecureRandom random = new SecureRandom(new byte[] { 0 });


	AuthStreams() : failure_flag(false), done_flag(false), inputbytesunread(0), waitingForIO(false) {
		authIn = new AuthInputStream(this);
		authOut = new AuthOutputStream(this);
	}

	~AuthStreams() {
		done_flag = true;
		if (authIn)
			delete authIn;
		if (authOut)
			delete authOut;
		{
			Lock lock(mux);
			if (protocolThread) {
				lock.notifyAll();
				usleep(100000);
				if (protocolThread) {
					protocolThread->stop();
				}
				lock.notifyAll();
			}
			while(protocolThread) {
				fprintf(stderr, "waiting for thread stop\n");
				lock.wait(2000);
			}
			fprintf(stderr, "thread is stopped\n");
		}
	}

	void startProtoThread() {
		Lock synch(mux);
		protocolThread = new Thread(this);
		protocolThread->start();
		if (!waitingForIO)
			waitForIO();
	}
	// entry point from SSH - packet received
	void receivePacket(byte *pkt, int len) {
		Lock synch(mux);
		Dio("receiving packet len " << len);

#if 1
		inputbytes.insert(inputbytes.end(), pkt, pkt+len);
		inputbytesunread += len;
#else 0
		if (inputbytes.empty()) {
			inputbytes.assign(pkt, pkt+len);;
			inputbytesunread = len;
		} else {
			byte_buf oldbytes;
			oldbytes.swap(inputbytes);
			inputbytes.resize(inputbytesunread+len);
			memcpy(&inputbytes[0], &oldbytes[oldbytes.size() - inputbytesunread - 1], inputbytesunread);
			memcpy(&inputbytes[inputbytesunread], pkt, len);
			inputbytesunread += len;
		}
#endif
		waitingForIO = false;
		synch.notifyAll();
		waitForIO();
	}

	void waitForIO() {
		Lock synch(mux);
		while(true) {
			Dio("looping...");
			while (authOut->buf.size() == 0 && !waitingForIO && !authOut->closed && !done_flag) {
				Dio("Thread sleeping...");
				synch.wait();
			}
			Dio("authOut waiting bytes: " << authOut->buf.size());
			if (authOut->buf.size() > 0) {

				Dio("Write a packet"); // outputbytes.size

				//byte_buf outputbytes(authOut->buf);
				//writePacket(outputbytes);
				writePacket(authOut->buf);
				authOut->buf.clear();
				Dio("Wrote a packet");
			}
			if (done_flag) {
				fprintf(stderr, "protocol done\n");
				return;
			}
			if (waitingForIO || !authOut || authOut->closed) {
				if(waitingForIO) {
					Dio("Waiting for input");
				}
				if (!authOut || authOut->closed) {
					if (authOut) {
						delete authOut;
						authOut = NULL;
					}
					if (!waitingForIO && !authIn)
						delete authIn;
						authIn = NULL;
				}
				if (!authOut && !authIn) {
					//protocolThread->stop();
				}
				synch.notifyAll();
#ifdef NDEBUG
				fprintf(stderr, "<");
#else
				Dio(stderr, "returning to native");
#endif
				return;
			}
		}
	}

    boolean writePacket(const byte_buf &b) {
    	return ssh_writePacket(&b[0], b.size());
    }

    virtual void go() = 0;
    virtual void start() = 0;

    static void cleanup(AuthStreams *that) {
    	Lock synch(that->mux);
    	that->protocolThread = NULL;
    	that->done_flag = true;
    	synch.notifyAll();
    }

    void run_with_cleanup() {
    	pthread_cleanup_push((void(*)(void*))(&cleanup), this);
    	try {
    		go();
    	} catch (...) {
			failure_flag = true;
			throw;
    	}
    	pthread_cleanup_pop_restore_np(true);
    }

    virtual void* run() {
    	try {
    		Dio("Thread starting...");
    		run_with_cleanup();
    		Lock lock(mux);
    		lock.notifyAll();
    	} catch (ProtocolException ex) {
    		fprintf(stderr,"protocol exception: %s\n", ex.what());
    		//failure_flag = true;
    		throw;
    	} catch (std::exception ex) {
    		fprintf(stderr, "exception: %s\n", ex.what());
    		//failure_flag = true;
    		throw;
    	} catch (...) {
    		fprintf(stderr, "unknown exception (pthread_cancel?)\n");
    		///failure_flag = true;		// could be a thread cancellation
    		//print_backtrace();
    		throw;
    	}
    	return NULL;
	}
};

inline int AuthOutputStream::tryWrite(const byte* c, int len) {
	Lock lock(streams->mux);
	int outlen = super::tryWrite(c, len);
	//lock.notifyAll();
	return outlen;
}


inline void AuthOutputStream::close() {
	Dio("output stream closing");
	super::close();
	Lock synch(streams->mux);
	streams->done_flag = true;
	closed = true;
	D("notifying close...");
	synch.notifyAll();
}

inline int AuthInputStream::tryRead(byte* b, int blen) {
	Lock synch(streams->mux);

	while (streams->inputbytes.empty() && !streams->done_flag) {
		Dio("wait to read");
		streams->waitingForIO = true;
		synch.notifyAll();
		synch.wait();
	}
//	if (streams->done_flag) {
//		throw ProtocolException("protocol aborted with exception");
//	}
	streams->waitingForIO = false;

	int off = streams->inputbytes.size() - streams->inputbytesunread;
	int len = min(streams->inputbytesunread, blen);
	memcpy(b, &streams->inputbytes[off], len);
	streams->inputbytesunread -= len;
	if (streams->inputbytesunread == 0)
		streams->inputbytes.clear();

	//System.err.println("returning read "+len+" bytes");
	return len;
}

#endif /* AUTHSTREAMS_H_ */
