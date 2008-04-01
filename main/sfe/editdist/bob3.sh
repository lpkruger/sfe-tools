cd ..
export CLASSPATH=build/sfe.jar
java -Xmx512m -DBOB -DBLOCKSIZE=$2 sfe.editdist.EDProtoBlock 3219 $1
