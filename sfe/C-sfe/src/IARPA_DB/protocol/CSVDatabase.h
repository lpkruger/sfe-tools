#ifndef __CSVDATABASE_H_
#define __CSVDATABASE_H_

#include "StringUtility.h"

#include <string>

using namespace std;

typedef string dbentry;
typedef vector<dbentry> rowentry;

class CSVDatabase {

protected:
  string attributesFile;
  vector<string> attributes;
  vector<rowentry> database;
  
public:
  CSVDatabase();
  CSVDatabase(string);
  CSVDatabase(string, vector<rowentry>);
  CSVDatabase(string, string);
  vector< vector<int> > getAttributeViews(string);
  vector<dbentry> getAttributeValues(string);
  rowentry getRow(int);
  dbentry getEntry(rowentry, string);
  string toString();
  char *toCString();
  int getAttributeNum(string);

  static unsigned char *entryData(dbentry);
  static unsigned int rowLength(rowentry);
  static unsigned int entryLength(dbentry);

};

#endif
