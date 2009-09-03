// ib.cpp
//
// "Isolated Box" party for PIR implementation.
// 2009 Matt Fredrikson, Louis Kruger

#include "PIRIsolatedBox.h"

// Collect encrypted, permuted views from the server
void PIRIsolatedBox::doServer(DataInput *in, DataOutput *out) {

  viewData.clear();
  viewSizes.clear();

  printf("got server connection\n");

  // The server will ask us about our cache
  unsigned int rAtt = in->readInt();

  // We consult, and report the results
  bool isCached = false;
  if(viewDataCache.count(rAtt) > 0) {
    isCached = true;
    out->writeByte(PROTOCOL_ACK);
  } else
    out->writeByte(PROTOCOL_NEG);

  // Make sure the server has the keys
  unsigned char srvCache = in->readByte();
  if(srvCache == PROTOCOL_NEG)
    isCached = false;

  // Consult the cache
  if(!isCached) {

    // Get the number of views
    unsigned int numViews = in->readInt();

    printf("reading %i views\n", numViews);
    
    // Get the encrypted views
    for(int i = 0; i < numViews; i++) {
      unsigned int curSize = in->readInt();
      unsigned char *curView = new unsigned char[curSize];
      in->readFully(curView, curSize);
      viewData.push_back(curView);
      viewSizes.push_back(curSize);
      
      printf("(%i, %i)", i, curSize);
    }
    printf("\n");
    
    viewDataCache.insert(make_pair(rAtt, viewData));
    viewSizesCache.insert(make_pair(rAtt, viewSizes));
    curAttribute = rAtt;
  } else {
    viewData = viewDataCache[rAtt];
    viewSizes = viewSizesCache[rAtt];
    printf("consulting cache for %i views\n", viewData.size());
  }

  delete in;
  delete out;
}

// Distribute the appropriate view to the client
void PIRIsolatedBox::doClient(DataInput *in, DataOutput *out) {

  printf("got client connection\n");

  // This is the view number requested by the client
  unsigned int rIndex = in->readInt();

  // This is the attribute number
  unsigned int rAtt = in->readInt();

  // If need be, load the correct viewset
  if(rAtt != curAttribute) {
    viewData = viewDataCache[rAtt];
    viewSizes = viewSizesCache[rAtt];
    curAttribute = rAtt;
  }

  printf("client request for view #%i\n", rIndex);

  unsigned char *rData = viewData[rIndex];
  unsigned int rSize = viewSizes[rIndex];

  // Send the encrypted view size
  out->writeInt(rSize);

  // Send the encrypted view data
  out->write(rData, rSize);

  printf("sent view of size %i to client\n", rSize);

  delete in;
  delete out;
}

// Runs while PIRIsolatedBox::runningServer == true
void PIRIsolatedBox::runServer() {
  ServerSocket ss(PORT);

  curAttribute = -1;

  runningServer = true;
  while(runningServer) {
    Socket *sock = ss.accept();
    DataOutput *out = sock->getOutput();
    DataInput *in = sock->getInput();

    // Is this the client or server contacting us?
    // Connecting party sends us an integer to let us know.
    int conRole = in->readByte();

    if(conRole == SERVER_ROLE)
	doServer(in, out);
    else if(conRole == CLIENT_ROLE)
	doClient(in, out);
  }
}

#ifdef MAIN_OVERLOAD
static int _main(int argc, char *argv[])
#else
int main(int argc, char *argv[])
#endif
{

  PIRIsolatedBox ib;

  ib.runServer();

  return 0;
}


#ifdef MAIN_OVERLOAD
#include "sillymain.h"
MAIN("PIRIsolatedBox")
#endif
