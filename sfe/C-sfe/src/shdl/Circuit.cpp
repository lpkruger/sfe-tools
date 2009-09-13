/*
 * Circuit.cpp
 *
 *  Created on: Sep 2, 2009
 *      Author: louis
 */

#include "shdl.h"

using namespace shdl;


void Circuit::calcDeps() {
    vector<Gate_p> stack;

    for (uint i=0; i<outputs.size(); ++i) {
    	stack.push_back(outputs[i]);
    }

    set<Gate_p> allGates;

    while (!stack.empty()) {
    	Gate_p gate = stack.back();
    	stack.pop_back();
    	if (allGates.find(gate) != allGates.end())
    		continue;


    	for (uint i=0; i<gate->inputs.size(); ++i) {
    		GateBase_p inp = gate->inputs[i];
    		if (!inp->isInput()) {
    			Gate_p inpg = static_cast<Gate*>(inp);
    			stack.push_back(inpg);
    		}
    		inp->deps.insert(gate);
    		allGates.insert(gate);
    	}
    }
}


void Circuit::clearDeps() {
    vector<Gate_p> stack;
    //printf("clearing...\n");

    for (uint i=0; i<outputs.size(); ++i) {
    	stack.push_back(outputs[i]);
    	outputs[i]->deps.clear();
    }

    set<Gate_p> allGates;

    while (!stack.empty()) {
    	Gate_p gate = stack.back();
    	stack.pop_back();

    	if (allGates.find(gate) != allGates.end())
    		continue;


    	for (uint i=0; i<gate->inputs.size(); ++i) {
    		GateBase_p inp = gate->inputs[i];

    		if (!inp->isInput()) {
    			Gate_p inpg = static_cast<Gate*>(inp);
    			stack.push_back(inpg);
    		}
    		inp->deps.clear();
    		allGates.insert(gate);
    	}
    }
}
