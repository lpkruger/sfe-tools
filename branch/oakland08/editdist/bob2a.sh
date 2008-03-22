cd ..
export CLASSPATH=build # build/sfe.jar
java -Xmx512m -DBOB -DCIRCUITONLY sfe.editdist.EDProto 3219 $1
