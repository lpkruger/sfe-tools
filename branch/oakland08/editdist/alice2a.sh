cd ..
export CLASSPATH=build # build/sfe.jar
java -Xmx512m -DALICE -DCIRCUITONLY sfe.editdist.EDProto $1 3219 $2
