/*
 * AuthStreams.h
 *
 *  Created on: Sep 4, 2009
 *      Author: louis
 */

#ifndef AUTHSTREAMS_H_
#define AUTHSTREAMS_H_

#define DEBUG 1
#include "sillyio.h"
#include "sillytype.h"
#include "../sillylib/os/sillythread.h"
#include <string>

#include "sillydebug.h"

using namespace silly::thread;
using namespace silly::io;
using namespace std;

///////////
class AuthStreams;

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
extern bool ssh_writePacket(byte *buf, int length);

class AuthInputStream : public DataInput {
	typedef DataInput super;
public:
	AuthStreams *streams;

	AuthInputStream(AuthStreams *str) {
		streams = str;
	}

	virtual int tryRead(byte* c, int len);

};


class AuthOutputStream : public BytesDataOutput {
	typedef BytesDataOutput super;
public:
	AuthStreams *streams;
    boolean closed;

	AuthOutputStream(AuthStreams *str) {
		streams = str;
	}

    void close();
};

struct AuthStreams : public Runnable {

	Mutex mux;
	boolean failure_flag;

	//static const boolean useMD5 = true;
	//static const boolean use_R = true;
	static const boolean useMD5 = true;
	static const boolean use_R = false;
	static const boolean useSeperateSocket = false;
	static const int num_circuits_default = 4;

#define Dio D

	// there will be 2 threads: One is Pure Java and executes the protocol,
	// the other is called from native code and goes back and forth
	AuthInputStream *authIn;
	AuthOutputStream *authOut;
	Thread *protocolThread;

	byte_buf outputbytes;
	byte_buf inputbytes;
	int inputbytesunread;
	boolean waiting;
	//SecureRandom random = new SecureRandom(new byte[] { 0 });


	AuthStreams() {
		authIn = new AuthInputStream(this);
		authOut = new AuthOutputStream(this);
	}

	void startProtoThread() {
		Lock synch(mux);
		protocolThread = new Thread(this);
		protocolThread->start();
		if (!waiting)
			waitForOutput();
	}
	// entry point from Native - packet received
	void receivePacket(byte *pkt, int len) {
		Lock synch(mux);
		Dio("receiving packet len ");

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
		waiting = false;
		mux.notifyAll();
		waitForOutput();
	}

	void waitForOutput() {
		Lock sync(mux);

		while(true) {
			Dio("looping...");
			while (authOut->buf.size() == 0 && !waiting && !authOut->closed && !failure_flag) {
				Dio("Thread sleeping...");
				mux.wait();
			}
			if (failure_flag) {
				printf("protocol aborted\n");
				return;
			}
			if (authOut->buf.size() > 0) {
				byte_buf outputbytes(authOut->buf);
				Dio("Write a packet"); // outputbytes.size
				writePacket(outputbytes);
				authOut->buf.clear();
				Dio("Wrote a packet");
			}
			if (waiting || authOut->closed) {
				if(waiting)
					Dio("Waiting for input");
				if (authOut->closed) {
					authOut = NULL;
					if (!waiting)
						authIn = NULL;
				}
				if (!authOut && !authIn)
					protocolThread->stop();
				Dio("returning to native");
				return;
			}

		}

	}

#ifndef DROPBEAR
	boolean writePacket(const byte_buf &b) {
		DC("wrtePacket: " << b.size());
		return true;
	}
#else
    boolean writePacket(const byte_buf &b) {
    	return ssh_writePacket(&b[0], b.size());
    }
#endif
    virtual void go() = 0;
    virtual void start() = 0;

    virtual void* run() {
		try {
			Dio("Thread starting...");
			go();
		} catch (...) {
			print_backtrace();
			Lock synch(mux);
			failure_flag = true;
			mux.notifyAll();
			throw;
		}
		// start Alice or Bob
	    return NULL;
	}

};

inline void AuthOutputStream::close() {
	Dio("output stream closing");
	super::close();
	Lock synch(streams->mux);
	closed = true;
	D("notifying...");
	streams->mux.notifyAll();
}

inline int AuthInputStream::tryRead(byte* b, int blen) {
	Lock synch(streams->mux);


	while (streams->inputbytes.empty() && !streams->failure_flag) {
		Dio("wait to read");
		streams->waiting = true;
		streams->mux.notifyAll();
		streams->mux.wait();
	}
	if (streams->failure_flag) {
		throw ProtocolException("protocol aborted");
	}
	streams->waiting = false;

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
