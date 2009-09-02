// StringUtility.cpp
//
// String utility functions.
// 2009 Matt Fredrikson, Louis Kruger

#include <vector>
#include <string>

#include "StringUtility.h"

using namespace std;

// Simple, stupid, additive
unsigned int StringUtility::hashBuf(const unsigned char * buf, int len) {
  unsigned int ret = 0;

  for(int i = 0; i < len; i++)
    ret = (ret + (unsigned int)buf[i]) % 2147483648;

  return ret;
}

// Takes a byte buffer and makes a BigInt out of it.
// Note: prepends 0xFF to rule out the chance of cutting off
// leading 0's.
BigInt StringUtility::bytes2BigInt(const unsigned char *buf, int len) {
  
  vector<unsigned char> bytes;
  bytes.push_back((unsigned char)0xFF);

  for(int i = 0; i < len; i++)
    bytes.push_back((unsigned char)buf[i]);

  return BigInt::toPaddedBigInt(bytes);
}

// Returns the bytes composing b as a new byte buffer, and returns
// the length of the buffer in len.
unsigned char *StringUtility::bigInt2Bytes(BigInt b, int *len) {
  
  vector<unsigned char> bytes = BigInt::fromPaddedBigInt(b); //.toPosByteArray();
  unsigned char *ret = new unsigned char[bytes.size()];

  vector<unsigned char>::iterator bi;
  for(bi = bytes.begin()+1; bi != bytes.end(); bi++)
    ret[bi-(bytes.begin()+1)] = *bi;

  *len = bytes.size()-1;
  return ret;
}

// Splits the string in str around delim, and returns the splits
// in a vector of strings.
// For example:
//  split(';', "hello;world!") = {"hello", "world!"}
vector<string> StringUtility::split(char delim, string str) {
  vector<string> ret;

  string::iterator oit;
  string curTok = "";
  boolean foundDelim = false;
  for(oit = str.begin(); oit != str.end(); oit++) {
    if(*oit == delim) {
	ret.push_back(curTok);
	curTok = "";
	foundDelim = true;

	continue;
    }

    curTok.append(1, *oit);
  }

  if(foundDelim)
    ret.push_back(curTok);

  return ret;
}


// Replaces each instance of origStr in str with newStr, returns the
// result.
string StringUtility::replace(string str, string origStr, string newStr) {
  string ret = str;

  bool finding = true;
  size_t idx = 0;
  while(finding) {
    idx = ret.find(origStr, 0);

    if(idx == string::npos)
	finding = false;
    else {
	ret = ret.replace(idx, origStr.length(), newStr, 0, newStr.length());
    }
    
    idx++;
  }

  return ret;
}


// Parses a CSV line.
// This entails little more than splitting a string around
// a comma, namely removing quotation marks from each field.
vector<string> StringUtility::parseCSVLine(string line) {

  vector<string> splitLine = split(',', line);
  vector<string> ret;

  vector<string>::iterator oit;
  for(oit = splitLine.begin(); oit != splitLine.end(); oit++)
    ret.push_back(replace(*oit, "\"", ""));

  return ret;
}
