#ifndef __AES_H_
#define __AES_H_

#include <openssl/evp.h>
#include <openssl/aes.h>

class AES {

protected:

  static const unsigned int DEFAULT_ROUNDS = 4;

  unsigned int rounds;
  unsigned char keyData[32];
  unsigned char key[32];
  unsigned char iv[32];

  EVP_CIPHER_CTX e_ctx;
  EVP_CIPHER_CTX d_ctx;

public:
  
  static const unsigned int KEYDATALEN = 32;

  // Default, no-arg constructor.
  // Generates a random key, and uses 8 rounds
  AES();
  // Takes key as the argument
  AES(unsigned char *);
  // Takes key, number of key gen rounds as the arguments
  AES(unsigned char *, unsigned int);
  unsigned char *encrypt(unsigned char *, int *len);
  unsigned char *decrypt(unsigned char *, int *len);
  static unsigned char *randomKey(unsigned int);

};

#endif
