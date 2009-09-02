#ifndef __PERMUTATION_H_
#define __PERMUTATION_H_

#include <vector>

using namespace std;

class Permutation {

protected:
  
  vector<int> perm;

public:
  
  Permutation(int);
  int permute(int);
  int inversePermute(int);
};

#endif

