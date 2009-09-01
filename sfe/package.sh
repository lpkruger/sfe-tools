set -x
# build sfeauth.zip
cd build/
zip -r sfe sfe/crypto sfe/shdl sfe/util sfe/sfauth -i \*.class
mv sfe.zip sfe.jar
cd ..
zip -j -m sfeauth build/sfe.jar
zip sfeauth md5_pw_cmp.[cf]*
zip sfeauth priveq.[cf]*
zip -j sfeauth dropbear/dropbear dropbear/dbclient dropbear/dropbearkey dropbear/*.[18] dropbear/README*
cd dropbear; make clean; cd ..
rm dropbear-src.zip
zip -r dropbear-src dropbear/ -x \*_key -x Makefile -x \*/Makefile -x \*/.svn -x \*/.svn/\*
zip sfeauth dropbear-src.zip




exit 0
# build release JAR for PPGT
cd build/
#export ZIPOPT="-D"
zip -r javabdd net -i \*.class
mv javabdd.zip javabdd.jar

#zip -r sfe sfe -i \*.class
zip -r sfe sfe/crypto sfe/editdist sfe/editdist2 sfe/editdistsw sfe/shdl sfe/util -i \*.class

mv sfe.zip sfe.jar

cd ..
rm PPGT.zip
zip -m PPGT build/sfe.jar
zip PPGT editdist/[ab]*[0-9].sh editdist/README* editdist/mkcirc*.sh editdist/proto2f*

cd build/

mkdir META-INF
cat > META-INF/MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: sfe.sfdl.CircuitCompiler
EOF

zip compiler META-INF META-INF/MANIFEST.MF
#zip compiler sfe sfe/sfdl sfe/shdl sfe/util
zip -r compiler sfe/sfdl sfe/shdl sfe/util -i \*.class
mv compiler.zip compiler.jar
mkdir examples
cp ../examples/*.txt examples/
cp ../fairplay_examples/*.txt examples/
cp -a ../smith-w examples/
cp ../README.txt .
cp ../run_* .

#rsync -zvrP -e ssh examples compile* sfe.jar README* run_* lpkruger@vetinari.cs.wisc.edu:public/compiler/
