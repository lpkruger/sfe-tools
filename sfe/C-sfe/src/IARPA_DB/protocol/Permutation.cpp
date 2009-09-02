
#include <stdlib.h>
#include <sys/time.h>

#include "Permutation.h"

Permutation::Permutation(int size) {
  vector<int> initial;
  struct timeval ctv;

  gettimeofday(&ctv, NULL);
  srand(ctv.tv_usec);

  for(int i = 0; i < size; i++)
    initial.push_back(i);

  while(initial.size() > 0) {
    int cur = rand() % initial.size();
    vector<int>::iterator ii = initial.begin() + cur;
    perm.push_back(*ii);
    initial.erase(ii);
  }
}

int Permutation::permute(int n) {
  return perm[n];
}

int Permutation::inversePermute(int n) {
  
  vector<int>::iterator i;
  for(i = perm.begin(); i != perm.end(); i++) {
    if(*i == n)
      return i - perm.begin();
  }

  return -1;
}
