#ifndef __PIRCLIENT_H_
#define __PIRCLIENT_H_

#include <map>
#include <utility>

#include "CSVDatabase.h"
#include "sillyio.h"
#include "bigint.h"
#include "DDB.h"

using namespace std;
using namespace silly::net;
using namespace bigint;
using namespace iarpa;

class PIRClient {

protected:

  static const unsigned char SERVER_ROLE = 1;
  static const unsigned char CLIENT_ROLE = 2;
  static const unsigned char KOT_READY = 3;
  static const unsigned char ATTRIBUTE_ERROR = 4;
  static const unsigned char PROTOCOL_ACK = 5;
  static const unsigned char PROTOCOL_NEG = 6;
  static const int KOTPORT = 5436;

  char *ibaddr;
  int ibport;
  char *srvaddr;
  int srvport;
  string attributesFile;

  map<unsigned char *, int> getKeyNumPair(BigInt);
  vector<rowentry> deBlobData(unsigned char *, int);

public:

  CSVDatabase queryServer(string, string);
  CSVDatabase doQuery(string);
  PIRClient(char *, int, char *, int, char *);

};

#endif
