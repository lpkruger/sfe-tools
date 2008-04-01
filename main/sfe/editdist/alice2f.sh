cd ..
export CLASSPATH=build #build/sfe.jar
java -Xmx512m -DALICE -DOTBATCH=$3 sfe.editdist.EDProto4 $1 3219 $2
