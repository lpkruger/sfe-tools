// CSVDatabase.cpp
//
// Wrapper object for comma-seperated value database file
// provided by MIT-LL.
// 2009 Matt Fredrikson, Louis Kruger

#include "CSVDatabase.h"
#include "StringUtility.h"

#include <string>
#include <istream>
#include <fstream>
#include <vector>
#include <map>
#include <utility>

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

using namespace std;

// This constructor won't work...
CSVDatabase::CSVDatabase() {
  return;
}

// Default constructor: takes two strings
// attributesList: filename of comma-separated (no quotations) list of attribute names
// databaseFile: filename of the MIT-LL CSV database
CSVDatabase::CSVDatabase(string attributesList, string databaseFile) {
  attributesFile = attributesList;

  // Load the attribute names into the vector
  ifstream attfin(attributesFile.c_str());
  char linebuf[4096];
  bzero(linebuf, 4096);
  attfin.getline(linebuf, 1024, '\n');
  attributes = StringUtility::split(',', string(linebuf));
  attfin.close();

  // Load the database into the vector
  ifstream dbfin(databaseFile.c_str());
  bzero(linebuf, 4096);
  while(dbfin.getline(linebuf, 4096, '\n')) {   
    database.push_back(StringUtility::parseCSVLine(string(linebuf)));

    rowentry c = StringUtility::parseCSVLine(string(linebuf));

    bzero(linebuf, 4096);
  }
  
}

// attributesList: filename of comma-separated (no quotations) list of attribute names
CSVDatabase::CSVDatabase(string attributesList) {
  attributesFile = attributesList;

  // Load the attribute names into the vector
  ifstream attfin(attributesFile.c_str());
  char linebuf[4096];
  bzero(linebuf, 4096);
  attfin.getline(linebuf, 1024, '\n');
  attributes = StringUtility::split(',', string(linebuf));
  attfin.close();

  database = vector<rowentry>();
}

// attributesList: filename of comma-separated (no quotations) list of attribute names
// initData: database is initialized to this value
CSVDatabase::CSVDatabase(string attributesList, vector<rowentry> initData) {
  attributesFile = attributesList;

  // Load the attribute names into the vector
  ifstream attfin(attributesFile.c_str());
  char linebuf[4096];
  bzero(linebuf, 4096);
  attfin.getline(linebuf, 1024, '\n');
  attributes = StringUtility::split(',', string(linebuf));
  attfin.close();

  database = initData;
}

// Creates a set of database views based on the possible values of a 
// given attribute.
// The return value is a vector of vector<int>'s. Each element of the vector is 
// a view, and each element of each view is an index into the database.
vector< vector<int> > CSVDatabase::getAttributeViews(string attribute) {
  vector< vector<int> > ret;
  map<string,int> vals;
  int viewnum = 0;

  // C++ vectors do not have a search procedure...
  int atnum = 0;
  vector<string>::iterator atit;
  for(atit = attributes.begin(); atit != attributes.end(); atit++) {
    if(*atit == attribute)
	break;
    atnum++;
  }

  // Didn't find that attribute...
  if(atnum >= attributes.size())
    return ret;

  vector<rowentry>::iterator dbit;
  int entnum = 0;
  for(dbit = database.begin(); dbit != database.end(); dbit++) {
    string curval = (*dbit)[atnum];

    // Add the current entry index to one view.
    if(vals.count(curval) == 0) {

	// We haven't seen this attribute value before.
	vals.insert(make_pair(curval, viewnum));
	vector<int> newview;
	newview.push_back(entnum);
	ret.push_back(newview);

	viewnum++;
    } else {
	int curvnum = vals[curval];
	ret[curvnum].push_back(entnum);
    }

    entnum++;
  }

  return ret;
}

// Returns a vector of values for a given attribute
vector<dbentry> CSVDatabase::getAttributeValues(string att) {
  vector<dbentry> ret;

  int atnum = 0;
  vector<string>::iterator atit;
  for(atit = attributes.begin(); atit != attributes.end(); atit++) {
    if(*atit == att)
      break;
    atnum++;
  }

  vector<rowentry>::iterator dbit;
  for(dbit = database.begin(); dbit != database.end(); dbit++) {
    dbentry cent = (*dbit)[atnum];

    bool hasEnt = false;
    vector<dbentry>::iterator rit;
    for(rit = ret.begin(); rit != ret.end(); rit++) {
      if(*rit == cent) {
	hasEnt = true;
	break;
      }
    }

    if(!hasEnt)
      ret.push_back(cent);
  }

  return ret;
}

dbentry CSVDatabase::getEntry(rowentry row, string att) {

  int atnum = 0;
  vector<string>::iterator atit;
  for(atit = attributes.begin(); atit != attributes.end(); atit++) {
    if(*atit == att)
      break;
    atnum++;
  }

  return row[atnum];
}

rowentry CSVDatabase::getRow(int rownum) {
  return database[rownum];
}

unsigned char *CSVDatabase::entryData(dbentry ent) {
  return (unsigned char *)ent.c_str();
}

unsigned int CSVDatabase::rowLength(rowentry ent) {
  return ent.size();
}

unsigned int CSVDatabase::entryLength(dbentry ent) {
  return ent.length();
}

string CSVDatabase::toString() {
  string ret=  "";

  vector<rowentry>::iterator dbit;
  for(dbit = database.begin(); dbit != database.end(); dbit++) {

    rowentry::iterator rit;
    for(rit = (*dbit).begin(); rit != (*dbit).end(); rit++) {
      int length = (*rit).length();
      char strlength[64];
      bzero((void *)strlength, 64);
      sprintf(strlength, "%d", length);
      ret = ret + attributes[rit-(*dbit).begin()] + "[" + string(strlength) + "]" + string((const char *)entryData(*rit));
    }
    ret = ret + "-";    
  }
  ret = ret + "=";

  return ret;
}

char *CSVDatabase::toCString() {
  string retStr = toString();

  return (char *)retStr.c_str();
}

int CSVDatabase::getAttributeNum(string attr) {
  
  vector<string>::iterator atit;
  for(atit = attributes.begin(); atit != attributes.end(); atit++) {
    if(*atit == attr)
      return atit - attributes.begin();
  }

  return -1;
}
