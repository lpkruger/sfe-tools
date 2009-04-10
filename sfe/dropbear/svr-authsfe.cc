extern "C" {
#include "includes.h"
#include "session.h"
#include "auth.h"
#include "dbutil.h"
#include "buffer.h"
#include "ssh.h"
#include "packet.h"
#include "runopts.h"
}
#include <jni.h>       /* where everything is defined */

static JavaVM *jvm;       /* denotes a Java VM */
static JNIEnv *env;       /* pointer to native method interface */
static jclass serverClass;
static jobject jserver;
static jmethodID receivePacket;
static jboolean inactive = true;

typedef const unsigned char cuchar;

JNIEXPORT jboolean JNICALL Java_sfe_sfauth_DropbearAuthStreams_writePacket
  (JNIEnv *env, jobject that, jbyteArray jbuf) {
    jint length = env->GetArrayLength(jbuf);
    jbyte* buf = env->GetByteArrayElements(jbuf, NULL);
    jbyte* p = buf;

    fprintf(stderr, "sending payload, len %d\n", length);

    int maxbuf = TRANS_MAX_PAYLOAD_LEN - 16;
    int n;

    while(length>0) {
      n = MIN(length, maxbuf);
      buf_putbyte(ses.writepayload, SSH_MSG_USERAUTH_SFEMSG);
      buf_putint(ses.writepayload, n);
      buf_putbytes(ses.writepayload, (cuchar*) p, n);
      encrypt_packet();
      fprintf(stderr, "*");
      p += n;
      length -= n;
    }

    env->ReleaseByteArrayElements(jbuf, buf, 0);
    return true;
}

void svr_auth_sfe_init() {
    JavaVMInitArgs vm_args; /* JDK/JRE 6 VM initialization arguments */
    JavaVMOption* options = new JavaVMOption[2];
    //options[0].optionString = "-Djava.class.path=/usr/lib/java";
    options[0].optionString = "-Xmx900m";
    //options[1].optionString = "-Djava.class.path=/usr/lib/jvm/sun-jdk-1.6/jre/lib/:/home/louis/sfe/build/:.";
    options[1].optionString = "-Djava.class.path=/etc/dropbear/sfe.jar";

    vm_args.version = JNI_VERSION_1_6;
    vm_args.nOptions = 2;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = false;
    /* load and initialize a Java VM, return a JNI interface
     * pointer in env */
    JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
    delete options;

    JNINativeMethod nMethods[1];
    nMethods[0].name = "writePacket";
    nMethods[0].signature = "([B)Z";
    nMethods[0].fnPtr = (void*) Java_sfe_sfauth_DropbearAuthStreams_writePacket;

    jclass authClass = env->FindClass("sfe/sfauth/DropbearAuthStreams");
    env->RegisterNatives(authClass, nMethods, 1);

    /* invoke the Main.test method using the JNI */
    serverClass = env->FindClass("sfe/sfauth/DropbearSfeServer");

    receivePacket = env->GetMethodID(serverClass, "receivePacket", "([B)V");
    //fprintf(stderr, "%x %x %x\n", serverClass, receivePacket, ctor);


    /* We are done. */
    //jvm->DestroyJavaVM();
}

typedef const unsigned char cuchar;

#if 0
static void write_packet(jbyteArray jbuf) {
    jint length = env->GetArrayLength(jbuf);
    jbyte* buf = env->GetByteArrayElements(jbuf, NULL);

    fprintf(stderr, "sending Java packet, len %d\n", length);

    buf_putbyte(ses.writepayload, SSH_MSG_USERAUTH_SFEMSG);
    buf_putint(ses.writepayload, length);
    buf_putbytes(ses.writepayload, (cuchar*) buf, length);
    encrypt_packet();

    env->ReleaseByteArrayElements(jbuf, buf, 0);
}
#endif

void svr_auth_sfe() {
  fprintf(stderr, "svr_auth_sfe\n");

  // copied from password authentication
#ifdef HAVE_SHADOW_H
	struct spwd *spasswd = NULL;
#endif
	char * passwdcrypt = NULL; /* the crypt from /etc/passwd or /etc/shadow */
	passwdcrypt = ses.authstate.pw_passwd;
#ifdef HAVE_SHADOW_H
	/* get the shadow password if possible */
	spasswd = getspnam(ses.authstate.pw_name);
	if (spasswd != NULL && spasswd->sp_pwdp != NULL) {
		passwdcrypt = spasswd->sp_pwdp;
	}
#endif

#ifdef DEBUG_HACKCRYPT
	/* debugging crypt for non-root testing with shadows */
	passwdcrypt = DEBUG_HACKCRYPT;
#endif

	/* check for empty password - need to do this again here
	 * since the shadow password may differ to that tested
	 * in auth.c */
	if (passwdcrypt[0] == '\0') {
		dropbear_log(LOG_WARNING, "user '%s' has blank password, rejected",
				ses.authstate.pw_name);
		send_msg_userauth_failure(0, 1);
		return;
	}

  // end copied from password authentication

  // startup the JVM
  fprintf(stderr, "svr_auth_sfe2\n");
  if (!env) {
    svr_auth_sfe_init();
  }
  // initialize an instance
  inactive = false;
  jstring jpasswdcrypt = env->NewStringUTF(passwdcrypt);
  if (!jpasswdcrypt) dropbear_exit("jpasswdcrypt is 0");

  jmethodID ctor = env->GetMethodID(serverClass, "<init>", "(Ljava/lang/String;)V");
  if (!ctor) dropbear_exit("ctor is 0");
  jserver = env->NewObject(serverClass, ctor, jpasswdcrypt);
  if (!jserver) dropbear_exit("jserver is 0");

  jmethodID start = env->GetMethodID(serverClass, "start", "()V");
  if (!start) dropbear_exit("start is 0");
  env->CallVoidMethod(jserver, start, jpasswdcrypt);
}

void recv_msg_userauth_sfemsg() {
  if (inactive) {
    fprintf(stderr, "%");
    return;
  }
  //fprintf(stderr, "recv_msg_userauth_sfemsg\n");
  fprintf(stderr, ".");
  jsize len = buf_getint(ses.payload);
  jbyte *buf = (jbyte*) buf_getptr(ses.payload, len);
  
  jbyteArray jbuf = env->NewByteArray(len);
  env->SetByteArrayRegion(jbuf, 0, len, buf);
  env->CallVoidMethod(jserver, receivePacket, jbuf);

  jfieldID failed = env->GetFieldID(serverClass, "failure_flag", "Z");
  if (env->GetBooleanField(jserver, failed)) {
      fprintf(stderr, "auth Exception failure\n");
      inactive = true;
      send_msg_userauth_failure(0, 1);
  }

  jfieldID done = env->GetFieldID(serverClass, "done", "Z");
  if (env->GetBooleanField(jserver, done)) {
    fprintf(stderr, "authentication is ");
    jfieldID success = env->GetFieldID(serverClass, "success", "Z");
    if (env->GetBooleanField(jserver, success)) {
      fprintf(stderr, "success\n");
      inactive = true;
      send_msg_userauth_success();
    } else {
      fprintf(stderr, "failure\n");
      inactive = true;
      send_msg_userauth_failure(0, 1);
    }
  } else {
    //fprintf(stderr, "authentication is not done\n");
    fprintf(stderr, "-");
  }


#if 0
  jeyteArray sbuf = (jbyteArray) env->CallObjectMethod(jserver, receivePacket, jbuf);
  if (sbuf) {
    write_packet(sbuf);
  }
#endif

}
