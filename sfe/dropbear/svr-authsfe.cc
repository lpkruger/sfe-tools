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

typedef unsigned char byte;
typedef const unsigned char cuchar;

bool inactive = true;

extern void new_sfe_server(char*);
extern void sfe_server_receive_packet(byte*,int);
extern bool sfe_server_get_failflag();
extern bool sfe_server_get_doneflag();
extern bool sfe_server_get_success();


bool ssh_writePacket(byte *buf, int length) {
    byte* p = buf;

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

    return true;
}


bool server_sfeauth_send_payload(byte *buf, int length) {
    byte* p = buf;
    fprintf(stderr, "sending payload, len %d\n", length);

    int maxbuf = TRANS_MAX_PAYLOAD_LEN - 16;
    int n;

    while(length>0) {
      n = MIN(length, maxbuf);
      buf_putbyte(ses.writepayload, SSH_MSG_USERAUTH_SFEMSG);
      buf_putint(ses.writepayload, n);
      buf_putbytes(ses.writepayload, p, n);
      encrypt_packet();
      fprintf(stderr, "*");
      p += n;
      length -= n;
    }

    return true;
}


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

  new_sfe_server(passwdcrypt);

}

void recv_msg_userauth_sfemsg() {
  if (inactive) {
    fprintf(stderr, "%%");
    return;
  }
  //fprintf(stderr, "recv_msg_userauth_sfemsg\n");
  fprintf(stderr, ".");
  int len = buf_getint(ses.payload);
  byte *buf = buf_getptr(ses.payload, len);
  
  sfe_server_receive_packet(buf, len);



  if (sfe_server_get_failflag()) {
      fprintf(stderr, "auth Exception failure\n");
      inactive = true;
      send_msg_userauth_failure(0, 1);
  }
  if (sfe_server_get_doneflag()) {
    if (sfe_server_get_success()) {
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
}



typedef int (*main_ptr)(int,char**);
void* add_main(const char* name, main_ptr main_f) {}
