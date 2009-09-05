/*
 * Cipher.cpp
 *
 *  Created on: Sep 1, 2009
 *      Author: louis
 */

#include "Cipher.h"
#include "EVPCipher.h"
#include "SFECipher.h"

using namespace crypto::cipher;

Cipher *Cipher::getInstance(string cipher) {
	if (cipher == "sfe")
		return new SFECipher();
	return new EVPCipher(cipher.c_str());
}

copy_counter SecretKey::counter("SecretKey");


#if 1		// test code
#include "EVPCipher.h"
#include "PseudoRandom.h"
#include "silly.h"

using silly::misc::toHexString;

static void usage() {
	printf("usage: cipher enc/dec algname key datastring\n");
	printf("       cipher list\n");
	printf("       cipher random key bytes [algname]\n");
	_exit(1);
}

static void list() {
	printf("Ciphers:\n");
	vector<string> list;
	list = EVPCipher::enumerate_ciphers();
	for (uint i=0; i<list.size(); ++i)
		printf("%s ", list[i].c_str());
	printf("\n\nDigests:\n");
	list = EVPCipher::enumerate_digests();
	for (uint i=0; i<list.size(); ++i)
		printf("%s ", list[i].c_str());
	printf("\n");
}

// usage: cipher enc/dec name data-string
static int _main(int argc, char **argv) {

	silly::mem::count_printer<byte_buf>   bytebuf_cnt;
	silly::mem::count_printer<CipherKey>  cipherkey_cnt;

	vector<string> args(argc-1);
	for (int i=1; i<argc; ++i) {
		args[i-1] = argv[i];
	}
	try {
		EVPCipher::init_all_algorithms();
		Cipher::modes mode;
		//printf("       cipher random key bytes [algname]\n");
		if (args.at(0) == "rand") {
			byte_buf key = byte_buf(args.at(1).begin(), args.at(1).end());
			EVPCipher *cipher = NULL;
			if (args.size() > 3) {
				cipher = new EVPCipher(args.at(3).c_str());
			}
			PseudoRandom rand(key, cipher);
			byte_buf out(strtol(args.at(2).c_str(), NULL, 0));
			rand.getBytes(out);
			printf("rand: %s\n", toHexString(out).c_str());
			return 0;
		} else if (args.at(0) == "list") {
			list();
			return 0;
		} else if (args.at(0) == "enc")
			mode = Cipher::ENCRYPT_MODE;
		else if (args.at(0) == "dec")
			mode = Cipher::DECRYPT_MODE;
		else
			usage();

		EVPCipher cipher(args.at(1).c_str());
		CipherKey key = byte_buf(args.at(2).begin(), args.at(2).end());
		printf("key: %s\n", toHexString(*key.getRawBuffer()).c_str());
		cipher.init(mode, &key);
		byte_buf input(args.at(3).begin(), args.at(3).end());
		printf("\n");
		printf("input : %s\n", toHexString(input).c_str());

		byte_buf output = cipher.doFinal(&input[0], input.size());

		printf("output: %s\n", toHexString(output).c_str());
		printf("\n");
		mode = (mode == Cipher::ENCRYPT_MODE ? Cipher::DECRYPT_MODE : Cipher::ENCRYPT_MODE);

		cipher.init(mode, &key);
		byte_buf again = cipher.doFinal(&output[0], output.size());
		printf("again : %s\n", toHexString(again).c_str());
	} catch (std::out_of_range) {
		usage();
	}
	return 0;
}

#include "sillymain.h"
MAIN("cipher")
#endif
