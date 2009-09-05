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

extern void new_sfe_client(char*);
extern void sfe_client_receive_packet(byte*,int);
extern bool sfe_client_get_failflag();

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
  inactive = false;

  CHECKCLEARTOWRITE();
  buf_putbyte(ses.writepayload, SSH_MSG_USERAUTH_REQUEST);
  buf_putstring(ses.writepayload, (cuchar*) cli_opts.username,
      strlen(cli_opts.username));

  buf_putstring(ses.writepayload, (cuchar*) SSH_SERVICE_CONNECTION,
      SSH_SERVICE_CONNECTION_LEN);
  buf_putstring(ses.writepayload, (cuchar*) AUTH_METHOD_SFE,
      AUTH_METHOD_SFE_LEN);
  encrypt_packet();

  new_sfe_client(password);

#if 0
  jbyteArray sbuf = (jbyteArray) env->CallObjectMethod(jclient, start);
  fprintf(stderr, "cli_auth_sfe2\n");
  if (sbuf) {
    write_packet(sbuf);
  }
#endif
    
  return 1;
}

extern "C" void recv_msg_userauth_sfemsg();

void recv_msg_userauth_sfemsg() {
  if (inactive) {
    fprintf(stderr, "%%");
    return;
  }
  //fprintf(stderr, "recv_msg_userauth_sfemsg\n");
  fprintf(stderr, ".");
  int len = buf_getint(ses.payload);
  byte *buf = buf_getptr(ses.payload, len);
  
  sfe_client_receive_packet(buf, len);

  if (sfe_client_get_failflag()) {
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

typedef int (*main_ptr)(int,char**);
void* add_main(const char* name, main_ptr main_f) {}

