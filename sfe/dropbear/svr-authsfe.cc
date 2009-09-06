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

extern void start_sfe_server(char*, int);
extern void stop_sfe_server();
extern void sfe_server_receive_packet(byte*,int);
extern bool sfe_server_get_failflag();
extern bool sfe_server_get_doneflag();
extern bool sfe_server_get_success();


bool ssh_writePacket(const unsigned char *buf, int length) {
    const byte *p = buf;

    //fprintf(stderr, "sending payload, len %d\n", length);
    fprintf(stderr, ">(%d) ", length);

    int maxbuf = TRANS_MAX_PAYLOAD_LEN - 16;
    int n;

    while(length>0) {
      n = MIN(length, maxbuf);
      buf_putbyte(ses.writepayload, SSH_MSG_USERAUTH_SFEMSG);
      buf_putint(ses.writepayload, n);
      buf_putbytes(ses.writepayload, (cuchar*) p, n);
      encrypt_packet();
      fprintf(stderr, "*", n);
      p += n;
      length -= n;
    }

    return true;
}

#if 0
bool server_sfeauth_send_payload(byte *buf, int length) {
    byte* p = buf;
    //fprintf(stderr, "sending payload, len %d\n", length);

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

  fprintf(stderr, "svr_auth_sfe2\n");
  
  char* num_circ_str = getenv("NUMCIRC");
  int num_circ = 0;
  if (num_circ_str)
    num_circ = strtol(num_circ_str, NULL, 0);

  start_sfe_server(passwdcrypt, num_circ);
  inactive = false;

}

void recv_msg_userauth_sfemsg() {
  if (inactive) {
    fprintf(stderr, "!");
    return;
  }
  //fprintf(stderr, "recv_msg_userauth_sfemsg\n");
  int len = buf_getint(ses.payload);
  byte *buf = buf_getptr(ses.payload, len);
  fprintf(stderr, ".{%d}", len);
  //fprintf(stderr, ".", len);

  //fprintf(stderr, "recv_msg_userauth_sfemsg len=%d\n", len);
  sfe_server_receive_packet(buf, len);



  if (sfe_server_get_failflag()) {
      fprintf(stderr, "auth Exception failure\n");
      stop_sfe_server();
      inactive = true;
      send_msg_userauth_failure(0, 1);
  } else if (sfe_server_get_doneflag()) {
    if (sfe_server_get_success()) {
      fprintf(stderr, "success\n");
      stop_sfe_server();
      inactive = true;
      send_msg_userauth_success();
    } else {
      fprintf(stderr, "failure\n");
      stop_sfe_server();
      inactive = true;
      send_msg_userauth_failure(0, 1);
    }
  } else {
    //fprintf(stderr, "authentication is not done\n");
    fprintf(stderr, "-");
  }
}

// to prevent link errors with SFE library
typedef int (*main_ptr)(int,char**);
void* add_main(const char* name, main_ptr main_f) {}
