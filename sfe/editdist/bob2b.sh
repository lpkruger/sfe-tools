cd ..
export CLASSPATH=build/sfe.jar
java -DBOB -DNBITS=80 sfe.editdist.EDProto 3219 $1
