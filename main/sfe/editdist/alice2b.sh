cd ..
export CLASSPATH=build/sfe.jar
java -DALICE -DNBITS=80 sfe.editdist.EDProto $1 3219 $2
