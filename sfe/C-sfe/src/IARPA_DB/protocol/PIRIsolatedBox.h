#ifndef __PIRISOLATEDBOX_H_
#define __PIRISOLATEDBOX_H_

#include <vector>
#include <map>
#include <utility>

#include "sillyio.h"

using namespace std;
using namespace silly::net;

class PIRIsolatedBox {

protected:

  static const unsigned char SERVER_ROLE = 1;
  static const unsigned char CLIENT_ROLE = 2;
  static const unsigned char KOT_READY = 3;
  static const unsigned char ATTRIBUTE_ERROR = 4;
  static const unsigned char PROTOCOL_ACK = 5;
  static const unsigned char PROTOCOL_NEG = 6;

  bool runningServer;
  unsigned int curAttribute;
  map<unsigned int, vector<unsigned char *> > viewDataCache;
  map<unsigned int, vector<int> > viewSizesCache;
  vector<unsigned char *> viewData;
  vector<int> viewSizes;

  void doClient(DataInput *, DataOutput *);
  void doServer(DataInput *, DataOutput *);

public:

  static const unsigned int PORT = 2222;

  void runServer();

};


#endif
