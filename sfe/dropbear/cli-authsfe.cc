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
#include "sfe_sfauth_DropbearAuthStreams.h"

static JavaVM *jvm;       /* denotes a Java VM */
static JNIEnv *env;       /* pointer to native method interface */
static jclass clientClass;
static jobject jclient;
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


static int cli_auth_sfe_init() {
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
    clientClass = env->FindClass("sfe/sfauth/DropbearSfeClient");
    receivePacket = env->GetMethodID(clientClass, "receivePacket", "([B)V");
    //fprintf(stderr, "%x %x %x\n", clientClass, receivePacket, ctor);

    /* We are done. */
    //jvm->DestroyJavaVM();
}


int cli_auth_sfe() {
        char* password = NULL;
        char prompt[80];

	snprintf(prompt, sizeof(prompt), "%s@%s's password: ", 
				cli_opts.username, cli_opts.remotehost);
#ifdef ENABLE_CLI_ASKPASS_HELPER
	if (want_askpass())
	{
		password = gui_getpass(prompt);
		if (!password) {
			dropbear_exit("No password");
		}
	} else
#endif
	{
		password = getpass_or_cancel(prompt);
	}


  fprintf(stderr, "cli_auth_sfe\n");
  if (!env) {
    cli_auth_sfe_init();
  }
  // initialize an instance
  inactive = false;
  jstring jpassword = env->NewStringUTF(password);
  jmethodID ctor = env->GetMethodID(clientClass, "<init>", "(Ljava/lang/String;)V");
  jclient = env->NewObject(clientClass, ctor, jpassword);

  CHECKCLEARTOWRITE();
  buf_putbyte(ses.writepayload, SSH_MSG_USERAUTH_REQUEST);
  buf_putstring(ses.writepayload, (cuchar*) cli_opts.username,
      strlen(cli_opts.username));

  buf_putstring(ses.writepayload, (cuchar*) SSH_SERVICE_CONNECTION,
      SSH_SERVICE_CONNECTION_LEN);
  buf_putstring(ses.writepayload, (cuchar*) AUTH_METHOD_SFE,
      AUTH_METHOD_SFE_LEN);
  encrypt_packet();

  jmethodID start = env->GetMethodID(clientClass, "start", "()V");
  env->CallVoidMethod(jclient, start);

#if 0
  jbyteArray sbuf = (jbyteArray) env->CallObjectMethod(jclient, start);
  fprintf(stderr, "cli_auth_sfe2\n");
  if (sbuf) {
    write_packet(sbuf);
  }
#endif
    
  return 1;
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
  env->CallVoidMethod(jclient, receivePacket, jbuf);


  jfieldID failed = env->GetFieldID(clientClass, "failure_flag", "Z");
  if (env->GetBooleanField(jclient, failed)) {
      inactive = true;
      fprintf(stderr, "Exception failure\n");
      cli_ses.state = USERAUTH_FAIL_RCVD;
  } else {
      fprintf(stderr, "-");
  }
#if 0
  jbyteArray sbuf = (jbyteArray) env->CallObjectMethod(jclient, receivePacket, jbuf);
  if (sbuf) {
    write_packet(sbuf);
  }
#endif
}
