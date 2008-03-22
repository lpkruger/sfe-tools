# build release JAR
set -x
cd build/
export ZIPOPT="-D"
zip -r javabdd net -i \*.class
mv javabdd.zip javabdd.jar

#zip -r sfe sfe -i \*.class
zip -r sfe sfe/crypto sfe/editdist sfe/shdl sfe/util -i \*.class

mv sfe.zip sfe.jar

cd ..
rm editdist_release.zip
zip editdist_release build/sfe.jar
zip editdist_release editdist/[ab]*[0-9].sh editdist/README* editdist/mkcirc*.sh editdist/proto2f*

cd build/

mkdir META-INF
cat > META-INF/MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: sfe.sfdl.CircuitCompiler
EOF

zip -r compiler sfe/sfdl sfe/shdl -i \*.class
zip compiler META-INF/MANIFEST.MF
mv compiler.zip compiler.jar
