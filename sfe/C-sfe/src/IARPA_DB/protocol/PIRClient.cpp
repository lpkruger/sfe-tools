// client.cpp
//
// Client party for PIR implementation.
// 2009 Matt Fredrikson, Louis Kruger

#include <string.h>

#include <iostream>

#include "PIRClient.h"
#include "AES.h"
#include "../KO.h"

using namespace std;


// Takes a BigInt that represents an AES key/permutation index
// pair, and returns a map with a single element representing that
// pair in convenience format.
map<unsigned char *, int> PIRClient::getKeyNumPair(BigInt b) {
  
  map<unsigned char *, int> ret;

  int len;
  unsigned char *bytes = StringUtility::bigInt2Bytes(b, &len);
  
  unsigned char *keyBytes = new unsigned char[AES::KEYDATALEN];
  memcpy((void *)keyBytes, (const void *)(bytes+1), AES::KEYDATALEN);

  int num;
  memcpy((void *)&num, (const void *)(bytes+AES::KEYDATALEN+1), sizeof(int));

  ret.insert(make_pair(keyBytes, num));
  return ret;
}

// De-blobs a block of bytes into a database view
vector<rowentry> PIRClient::deBlobData(unsigned char *data, int len) {
  vector<rowentry> ret;

  int consumed = 0;
  int numents;

  memcpy((void *)&numents, (const void *)data, sizeof(int));
  data += sizeof(int);
  //printf("de-blobbing %i rows\n", numents);

  while(consumed < numents) {    
    unsigned int curRowLen = *((unsigned int *)data);
    data += sizeof(unsigned int);
    len += sizeof(unsigned int);
    
    vector<dbentry> curRow;
    
    for(int i = 0; i < curRowLen; i++) {      
      unsigned int curEntLen = *((unsigned int *)data);
      data += sizeof(unsigned int);
      len += sizeof(unsigned int);

      unsigned char *curEnt = new unsigned char[curEntLen+1];
      memcpy((void *)curEnt, (const void *)data, curEntLen);
      curEnt[curEntLen] = 0;
      data += curEntLen;
      len += curEntLen;

      curRow.push_back(dbentry((const char *)curEnt));
    }

    consumed++;
    ret.push_back(curRow);
  }
  
  return ret;
}

CSVDatabase PIRClient::queryServer(string attr, string val) {
  
  //printf("connecting to %s on port %i\n", srvaddr, srvport);
  Socket *srvSock = new Socket(srvaddr, srvport);

  DataOutput *srvOut = srvSock->getOutput();
  DataInput *srvIn = srvSock->getInput();

  //printf("sending query attribute \"%s\" to server\n", attr.c_str());

  // C informs S which attribute it is about to query on
  srvOut->writeInt(attr.length());
  srvOut->write((unsigned char *)attr.c_str(), attr.length());

  // Make sure that the server can honor our query
  unsigned char isAck = srvIn->readByte();
  if(isAck != PROTOCOL_ACK) {
    // There was an error on the server side.
    // Close up, and return an empty database.
    delete srvOut;
    delete srvIn;
    delete srvSock;

    return CSVDatabase(attributesFile);
  }

  // Wait for notification that KOT is ready
  unsigned char kotReady = srvIn->readByte();

  //printf("starting KOT with \"%s\" on port %i for key \"%s\"\n", srvaddr, KOTPORT, val.c_str());

  // C and S perform an oblivious transfer step
  Socket s(srvaddr, KOTPORT);
  DataOutput *sKotOut = s.getOutput();
  DataInput *sKotIn = s.getInput();
  ko::Client cli;
  cli.setStreams(sKotIn, sKotOut);
  byte_buf netKoResult = cli.online(StringUtility::bytes2BigInt((unsigned char *)val.c_str(), val.length()));
  // If netKoResult has not bytes, then a match was not found
  if(netKoResult.size() == 0)
    return CSVDatabase(attributesFile);
  BigInt result = BigInt::toPaddedBigInt(netKoResult);
  map<unsigned char *, int> kotOutM = getKeyNumPair(result);
  map<unsigned char *, int>::iterator kotOutP = kotOutM.begin();
  unsigned char *k = (*kotOutP).first;
  int sigmai = (*kotOutP).second;

  //printf("oblivious transfer completed\ncontacting IB to retrieve encrypted view #%i\n", sigmai);

  // C asks IB for the sigmai-th element in the list of ciphertexts
  Socket ibSock(ibaddr, ibport);
  DataOutput *ibOut = ibSock.getOutput();
  DataInput *ibIn = ibSock.getInput();

  // Tell the IB what we're up to
  ibOut->writeByte(CLIENT_ROLE);

  // Send the desired index to the IB
  ibOut->writeInt(sigmai);

  // Send the attribute number to the IB
  ibOut->writeInt(StringUtility::hashBuf((const unsigned char *)attr.c_str(), attr.length()));

  // Get the encrypted view size from the IB
  unsigned int dataSize = ibIn->readInt();

  //printf("reading view of size %i\n", dataSize);

  // Get the encrypted view data from the IB
  unsigned char *cipherText = new unsigned char[dataSize];
  ibIn->readFully(cipherText, dataSize);

  //printf("transfer complete\n");

  // Unencrypt the ciphertext
  AES E = AES(k);
  unsigned char *plainText = E.decrypt(cipherText, (int *)&dataSize);

  //printf("decrypted view, %i bytes, key: %i\n", dataSize, StringUtility::hashBuf(k, AES::KEYDATALEN));

  // And un-blob it
  vector<rowentry> queryView = deBlobData(plainText, dataSize);

  // Clean up
  delete srvOut;
  delete srvIn;
  delete srvSock;
  delete ibOut;
  delete ibIn;
  delete cipherText;
  delete plainText;
  delete sKotOut;
  delete sKotIn;

  // And return a fresh database
  return CSVDatabase(attributesFile, queryView);
}

PIRClient::PIRClient(char *iba, int ibp, char *srva, int srvp, char *attf) {
  ibaddr = iba;
  ibport = ibp;
  srvaddr = srva;
  srvport = srvp;
  attributesFile = string(attf);
}

CSVDatabase PIRClient::doQuery(string sqlQuery) {

  sqlQuery = StringUtility::replace(sqlQuery, "select * from sample where ", "");
  sqlQuery = StringUtility::replace(sqlQuery, ";", "");
  sqlQuery = StringUtility::replace(sqlQuery, " ", "");
  sqlQuery = StringUtility::replace(sqlQuery, "\'", "");

  vector<string> eqSplit = StringUtility::split('=', sqlQuery);

  //printf("parsed query: %s == %s\n", eqSplit[0].c_str(), eqSplit[1].c_str());

  return queryServer(eqSplit[0], eqSplit[1]);
}

// args: 1 = isolated box address
//       2 = isolated box port
//       3 = server address
//       4 = server port
//       5 = attributes file
#ifdef MAIN_OVERLOAD
static int _main(int argc, char *argv[])
#else
int main(int argc, char *argv[])
#endif
{
	if (argc!=6) {
		printf("Usage: PIRClient (isolated box address) (isolated box port)\n");
		printf("                 (server address) (server port) (attributes file)\n");
		return 1;
	}
  PIRClient client(argv[1], atoi(argv[2]), argv[3], atoi(argv[4]), argv[5]);

//   CSVDatabase result = client.doQuery("select * from sample where State = \'NY\'");
//   //printf("\n\nquery result:\n%s\n", result.toCString());

  boolean querying = true;
  while(querying) {
    char stdinp[4096];
    bzero((void *)stdinp, 4096);

    printf("\r\n>> ");
    cin.getline(stdinp, 4096, '\n');

    if(!strcmp(stdinp, "DONE"))
      break;

    CSVDatabase result = client.doQuery(string(stdinp));
    string output = result.toString();
    printf("%s\n", output.c_str());
  }
  
  return 0;
}

#ifdef MAIN_OVERLOAD
#include "sillymain.h"
MAIN("PIRClient")
#endif
