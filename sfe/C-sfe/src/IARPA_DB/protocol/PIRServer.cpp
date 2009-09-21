// server.cpp
//
// Server party for PIR implementation.
// 2009 Matt Fredrikson, Louis Kruger

#include <stdlib.h>
#include <sys/time.h>

#include "PIRServer.h"
#include "AES.h"
#include "Permutation.h"
#include "../../crypto/ot/KurosawaOgata.h"

// Takes a set of database rows (a view), and builds a network "blob" of data.
// The format of the data is:
// <num rows>
// <row length> <entry length> <entry data> <entry length> <entry data> ...
// <row length> <entry length> <entry data> <entry length> <entry data> ...
// .
// .
// .
// Returns the length of the blob in len.
unsigned char *PIRServer::prepareNetworkBlob(vector<rowentry> entries, int *len) {
  unsigned char *ret;
  int curpos = 0;
  int numrows = entries.size();
  
  *len = sizeof(int);

  // First get the size of the data
  vector<rowentry>::iterator ri;
  for(ri = entries.begin(); ri != entries.end(); ri++) {
    rowentry::iterator ei;
    for(ei = (*ri).begin(); ei != (*ri).end(); ei++)
      *len += CSVDatabase::entryLength(*ei) + sizeof(unsigned int);
    *len += sizeof(unsigned int);
  }

  ret = new unsigned char[*len];

  // Insert the number of rows
  memcpy((void *)ret, (const void *)&numrows, sizeof(int));
  curpos += sizeof(int);

  // Now copy the data into the buffer
  for(ri = entries.begin(); ri != entries.end(); ri++) {
    unsigned int rowLength = CSVDatabase::rowLength(*ri);
    memcpy((void *)(ret+curpos), (const void *)&rowLength, sizeof(unsigned int));
    curpos += sizeof(unsigned int);

    rowentry::iterator ei;
    for(ei = (*ri).begin(); ei != (*ri).end(); ei++) {
      unsigned int entryLength = CSVDatabase::entryLength(*ei);
      memcpy((void *)(ret+curpos), (const void *)&entryLength, sizeof(unsigned int));
      memcpy((void *)(ret+curpos+sizeof(unsigned int)), (const void *)CSVDatabase::entryData(*ei), entryLength);

      curpos += entryLength + sizeof(unsigned int);
    }

  }

  return ret;
}


DDB PIRServer::makeKOTDatabase(vector<string> atVals, vector<unsigned char *> keys, Permutation sigma) {
  DDB ret;

  // atVals and keys must have the same number of elements
  if(atVals.size() != keys.size())
    return ret;

  // Make BigInt's for elements in atVals
  vector<BigInt> biAtVals;
  vector<string>::iterator ai;
  for(ai = atVals.begin(); ai != atVals.end(); ai++)
    biAtVals.push_back(StringUtility::bytes2BigInt((unsigned char *)(*ai).c_str(), (*ai).length()));
   
  // Make BigInt's for <k, \sigma> pairs
  int curNum = 0;
  vector<BigInt> biKeyData;
  vector<unsigned char *>::iterator ki;
  for(ki = keys.begin(); ki != keys.end(); ki++) {
    unsigned char *curKey = *ki;
    byte_buf bytes;

    // Put a dummy byte in to make sure any leading 0's aren't cut off
    bytes.push_back((unsigned char)0xFF);

    // Put the key data
    for(int i = 0; i < AES::KEYDATALEN; i++)
      bytes.push_back(curKey[i]);

    // Put in sigma inverse
    int sinv = sigma.inversePermute(curNum);
    for(int i = 0; i < sizeof(int); i++)
      bytes.push_back((unsigned char)*(((unsigned char *)&sinv)+i));

    biKeyData.push_back(BigInt::toPaddedBigInt(bytes));

    curNum++;
  }

  // Now place the elements in the database
  for(int i = 0; i < biAtVals.size(); i++)
    ret.put(biAtVals[i], biKeyData[i]);

  return ret;
}

int PIRServer::serveQuery(DataInput *clIn, DataOutput *clOut) {

  // First get the desired attribute from the client
  unsigned int attrSize = clIn->readInt();
  printf("got size: %i\n", attrSize);
  unsigned char *attrBuf = new unsigned char[attrSize+1];
  clIn->readFully(attrBuf, attrSize);
  attrBuf[attrSize] = 0;
  string attribute((const char *)attrBuf);
  delete attrBuf;

  printf("got attribute request %s\n", attribute.c_str());

  // Make sure query is serviceable
  if(database.getAttributeNum(attribute) < 0) {
    printf("attribute does not exist!\n");
    clOut->writeByte(ATTRIBUTE_ERROR);
    delete clIn;
    delete clOut;

    return -1;
  } else
    clOut->writeByte(PROTOCOL_ACK);

  // Connect to the IB
  Socket ibSock(ibaddr, ibport);
  DataOutput *ibOut = ibSock.getOutput();
  DataInput *ibIn = ibSock.getInput();

  // Tell the IB our intentions
  ibOut->writeByte(SERVER_ROLE);

  // Ask the IB if he's got this attribute in his cache
  boolean isCached = false;
  unsigned int attHash = StringUtility::hashBuf((const unsigned char *)attribute.c_str(), attribute.length());
  ibOut->writeInt(attHash);
  unsigned char ibCache = ibIn->readByte();
  if(ibCache == PROTOCOL_ACK)
    isCached = true;
  else
    isCached = false;

  // See if the keyword is locally-cached
  if(keysCache.count(attHash) <= 0) {
    // Tell the IB to listen for some new information,
    // because we don't have it saved.
    ibOut->writeByte(PROTOCOL_NEG);
    isCached = false;
  } else
    ibOut->writeByte(PROTOCOL_ACK);

  vector< vector<int> > D;
  vector<unsigned char *> k;
  vector<unsigned char *> c;
  vector<unsigned int> clens;
  Permutation *perm;
  int m;
  if(!isCached) {

    // S creates m views D1,...,Dm of the database D
    D = database.getAttributeViews(attribute);
    m = D.size();

    // Pick m random keys k1,...km from the key space
    printf("Generating %i random keys\n", m);

    for(int i = 0; i < m; i++) {
      struct timeval ctv;
      gettimeofday(&ctv, NULL);
      
      unsigned char *newKey = AES::randomKey(ctv.tv_usec);
      k.push_back(newKey);
      
      AES E = AES(newKey);
      
      // S prepares m ciphertexts from the views
      vector<int> curView = D[i];
      vector<rowentry> curData;
      vector<int>::iterator cRow;
      for(cRow = curView.begin(); cRow != curView.end(); cRow++)
	curData.push_back(database.getRow(*cRow));
      int viewLen;
      unsigned char *viewData = prepareNetworkBlob(curData, &viewLen);
      c.push_back(E.encrypt(viewData, &viewLen));
      clens.push_back((unsigned int)viewLen);
    }

    // Pick a random permutation \sigma of m elements
    perm = new Permutation(m);

    // Feed the cache
    keysCache.insert(make_pair(attHash, k));
    permsCache.insert(make_pair(attHash, perm));
  } else {
    // Consult the cache
    k = keysCache[attHash];
    perm = permsCache[attHash];
    m = k.size();
  }

  if(!isCached) {
    printf("sending %i views to isolated box\n", m);
    
    // Send the number of views to the IB
    ibOut->writeInt(m);

    // Send the ciphertexts (in permuted order) to the IB
    for(int i = 0; i < m; i++) {
      ibOut->writeInt(clens[perm->permute(i)]);
      ibOut->write(c[perm->permute(i)], clens[perm->permute(i)]);
    }
  }

  printf("initiating oblivious transfer on port %i\n", KOTPORT);

  // Now perform the oblivious transfer step with the client
  crypto::ot::ko::Sender kotServer;
  DDB ddb = makeKOTDatabase(database.getAttributeValues(attribute), k, *perm);;
  kotServer.precompute(ddb.thedb);
  // Tell the client we're ready for the KOT
  clOut->writeByte(KOT_READY);
  // Wait for the client's KOT initiation...
  Socket *cliOtSock = kotSS->accept();
  DataOutput *kotOut = cliOtSock->getOutput();
  DataInput *kotIn = cliOtSock->getInput();
  kotServer.setStreams(kotIn, kotOut);
  kotServer.online();

  // Clean up
  delete ibIn;
  delete ibOut;
  delete clIn;
  delete clOut;
  delete kotIn;
  delete kotOut;
  delete cliOtSock;
  vector<unsigned char *>::iterator ci;
  for(ci = c.begin(); ci != c.end(); ci++)
    delete *ci;

  printf("completed oblivious transfer\nquery on attribute %s served.\n", attribute.c_str());

  // 1 for success...
  return 1;
}

// Serves new requests as long as serving == true
void PIRServer::waitForQueries() {
  ServerSocket *ss = new ServerSocket(PORT);

  printf("PIRServer waiting on port %i\n", PORT);

  while(serving) {
    Socket *s = ss->accept();
    DataInput *clIn = s->getInput();
    DataOutput *clOut = s->getOutput();

    printf("Received connection.\n");

    serveQuery(clIn, clOut);

    delete s;
  }

  delete ss;
}

PIRServer::PIRServer(char *iba, int ibp, string dbfile, string dbafile) {
  ibaddr = iba;
  ibport = ibp;
  database = CSVDatabase(dbafile, dbfile);
  kotSS = new ServerSocket(KOTPORT);
  serving = true;
}

// args: 1 = isolated box address
//       2 = isolated box port
//       3 = database file
//       4 = database attributes file
#ifdef MAIN_OVERLOAD
static int _main(int argc, char *argv[])
#else
int main(int argc, char *argv[])
#endif
{
  // TODO: fix arg handling
  if (argc!=5) {
	  printf("Usage: PIRServer (isolated box address) (isolated box port)\n");
	  printf("                 (database file) (database attributes file)\n");
	  return 1;
  }
  PIRServer server(argv[1], atoi(argv[2]), string(argv[3]), string(argv[4]));

  server.waitForQueries();

  return 0;
}


#ifdef MAIN_OVERLOAD
#include "sillymain.h"
MAIN("PIRServer")
#endif
