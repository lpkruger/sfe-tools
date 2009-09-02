
#ifndef __STRINGUTILITY_H_
#define __STRINGUTILITY_H_

#include <vector>
#include <string>

#include "bigint.h"

using namespace std;
using namespace bigint;

class StringUtility {

public:

  static vector<string> split(char, string);
  static vector<string> parseCSVLine(string);
  static string replace(string, string, string);
  static BigInt bytes2BigInt(const unsigned char *, int);
  static unsigned char *bigInt2Bytes(BigInt, int*);
  static unsigned int hashBuf(const unsigned char *, int);

};

#endif
