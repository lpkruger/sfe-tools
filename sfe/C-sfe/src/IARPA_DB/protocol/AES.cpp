// AES.cpp

#include <string.h>
#include <stdlib.h>

#include <sys/time.h>

#include "AES.h"

// Default, no-arg constructor.
// Generates a random key, and uses 8 rounds
AES::AES() {
  struct timeval ctv;
  gettimeofday(&ctv, NULL);
  unsigned char *newKey = randomKey(ctv.tv_usec);
  AES(newKey, DEFAULT_ROUNDS);
}

// Takes key as the argument
AES::AES(unsigned char *pKey) {
  memcpy(keyData, pKey, 32*sizeof(unsigned char));
  rounds = DEFAULT_ROUNDS;

  // Generate the key and IV for AES 256 CBC with rounds SHA1.
  EVP_BytesToKey(EVP_aes_256_cbc(), EVP_sha1(), NULL, keyData, 32*sizeof(unsigned char), rounds, key, iv);

  EVP_CIPHER_CTX_init(&e_ctx);
  EVP_EncryptInit_ex(&e_ctx, EVP_aes_256_cbc(), NULL, key, iv);
  EVP_CIPHER_CTX_init(&d_ctx);
  EVP_DecryptInit_ex(&d_ctx, EVP_aes_256_cbc(), NULL, key, iv);
}

// Takes key (32 uchars wide), number of key gen rounds as the arguments
AES::AES(unsigned char *pKey, unsigned int numRounds) {
  memcpy(keyData, pKey, 32*sizeof(unsigned char));
  rounds = numRounds;

  // Generate the key and IV for AES 256 CBC with rounds SHA1.
  EVP_BytesToKey(EVP_aes_256_cbc(), EVP_sha1(), NULL, keyData, 32*sizeof(unsigned char), rounds, key, iv);

  EVP_CIPHER_CTX_init(&e_ctx);
  EVP_EncryptInit_ex(&e_ctx, EVP_aes_256_cbc(), NULL, key, iv);
  EVP_CIPHER_CTX_init(&d_ctx);
  EVP_DecryptInit_ex(&d_ctx, EVP_aes_256_cbc(), NULL, key, iv);
}

unsigned char *AES::encrypt(unsigned char *plaintext, int *len) {
  int clen = *len + AES_BLOCK_SIZE;
  int flen = 0;
  unsigned char *ret = new unsigned char[clen];

  bzero((void *)ret, clen);

  EVP_EncryptInit_ex(&e_ctx, NULL, NULL, NULL, NULL);
  EVP_EncryptUpdate(&e_ctx, ret, &clen, plaintext, *len);
  EVP_EncryptFinal_ex(&e_ctx, ret + clen, &flen);

  *len = clen + flen;

  return ret;
}

unsigned char *AES::decrypt(unsigned char *ciphertext, int *len) {
  int plen = *len;
  int flen = 0;
  unsigned char *ret = new unsigned char[plen];

  bzero((void *)ret, plen);

  EVP_DecryptInit_ex(&d_ctx, NULL, NULL, NULL, NULL);
  EVP_DecryptUpdate(&d_ctx, ret, &plen, ciphertext, *len);
  EVP_DecryptFinal_ex(&d_ctx, ret + plen, &flen);

  *len = plen + flen;

  return ret;
}

// Generates 32 bytes of random key data
unsigned char *AES::randomKey(unsigned int seed) {
  unsigned char *ret = new unsigned char[32];
  
  srand(seed);
  for(int i = 0; i < 32; i++)
    ret[i] = rand()%256;

  return ret;
}
