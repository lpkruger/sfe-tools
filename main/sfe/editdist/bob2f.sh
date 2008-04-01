cd ..
export CLASSPATH=build #build/sfe.jar
java -Xmx512m -DBOB -DOTBATCH=$2 sfe.editdist.EDProto4 3219 $1
