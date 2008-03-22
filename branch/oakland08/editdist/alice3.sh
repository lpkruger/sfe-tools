cd ..
export CLASSPATH=build/sfe.jar
java -Xmx512m -DALICE -DBLOCKSIZE=$3 sfe.editdist.EDProtoBlock $1 3219 $2
