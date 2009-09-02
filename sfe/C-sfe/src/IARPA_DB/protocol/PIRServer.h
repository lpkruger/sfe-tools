#ifndef __PIRSERVER_H_
#define __PIRSERVER_H_

#include <string>
#include <vector>
#include <map>
#include <utility>

#include "Permutation.h"
#include "CSVDatabase.h"
#include "sillyio.h"
#include "bigint.h"
#include "DDB.h"

using namespace std;
using namespace silly::net;
using namespace bigint;
using namespace iarpa;

class PIRServer {

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
  boolean serving;
  CSVDatabase database;
  ServerSocket *kotSS;

  // Cache stuff
  map<unsigned int, vector<unsigned char *> > keysCache;
  map<unsigned int, Permutation *> permsCache;
  
  unsigned char *prepareNetworkBlob(vector<rowentry>, int *);
  DDB makeKOTDatabase(vector<string>, vector<unsigned char *>, Permutation);

public:

  static const int PORT = 3333;
 
  int serveQuery(DataInput *, DataOutput *);
  void waitForQueries();
  PIRServer(char *, int, string, string);
  
};

#endif
