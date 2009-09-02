
# Directories
INCLUDE=../../../include
CRYPTOCODE=../../crypto
IARPA_DB=../
SILLYDIR=../../sillylib
SILLYMATH=../../sillylib/math

# Commands and switches
CPP=g++-4.3
# CPP=/s/gcc-4.3.2/bin/g++
CFLAGS=-O0 -std=c++0x -g -I$(INCLUDE)
SSLFLAGS=-lcrypto

all: CSVDatabase.o PIRClient PIRIsolatedBox PIRServer

PIRClient: PIRClient.cpp AES.o CSVDatabase.o StringUtility.o Permutation.o silly.o KO.o BigInt.o
	$(CPP) $(CFLAGS) $(SSLFLAGS) -I$(IARPA_DB) PIRClient.cpp AES.o CSVDatabase.o StringUtility.o Permutation.o silly.o KO.o BigInt.o -o PIRClient

PIRIsolatedBox: PIRIsolatedBox.cpp AES.o silly.o
	$(CPP) $(CFLAGS) $(SSLFLAGS) PIRIsolatedBox.cpp silly.o -o PIRIsolatedBox

PIRServer: CSVDatabase.o PIRServer.cpp StringUtility.o AES.o Permutation.o silly.o KO.o BigInt.o
	$(CPP) $(CFLAGS) $(SSLFLAGS) -I$(IARPA_DB) PIRServer.cpp AES.o CSVDatabase.o StringUtility.o Permutation.o KO.o BigInt.o silly.o -o PIRServer

CSVDatabase.o: CSVDatabase.cpp StringUtility.o
	$(CPP) $(CFLAGS) -c CSVDatabase.cpp

StringUtility.o: StringUtility.cpp BigInt.o
	$(CPP) $(CFLAGS) -c StringUtility.cpp

AES.o: AES.cpp
	$(CPP) $(CFLAGS) -c AES.cpp

Permutation.o: Permutation.cpp
	$(CPP) $(CFLAGS) -c Permutation.cpp

silly.o: $(SILLYDIR)/silly.cpp
	$(CPP) $(CFLAGS) -c $(SILLYDIR)/silly.cpp

KO.o: $(IARPA_DB)/KO.cpp
	$(CPP) $(CFLAGS) -c $(IARPA_DB)/KO.cpp

BigInt.o: $(SILLYMATH)/BigInt.cpp
	$(CPP) $(CFLAGS) -c $(SILLYMATH)/BigInt.cpp

clean:
	rm -rf *.o PIRClient PIRIsolatedBox PIRServer CSVDatabase.o
