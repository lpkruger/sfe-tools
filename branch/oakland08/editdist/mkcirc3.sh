m=$1
n=$2

cat << END
program edit_${m}_${n} {
        const N=8;
        type Num = Int<N>;
        type Char = Int<8>;
	type StrA = Char[$m];
	type StrB = Char[$n];
END

echo -n "        type AliceInput = struct { StrA x"
for ((i=0; i<=m; ++i)) ; do
	echo -n ", Num dd_${i}_0_a"
done
for ((j=1; j<=n; ++j)) ; do
	echo -n ", Num dd_0_${j}_a"
done
for ((i=1; i<=m; ++i)) ; do
	echo -n ", Num out_${i}_${n}_a"
done
for ((j=1; j<=n-1; ++j)) ; do
	echo -n ", Num out_${m}_${j}_a"
done

echo " };"

echo -n "        type BobInput = struct { StrB x" 
for ((i=0; i<=m; ++i)) ; do
	echo -n ", Num dd_${i}_0_b"
done
for ((j=1; j<=n; ++j)) ; do
	echo -n ", Num dd_0_${j}_b"
done
echo " };"


echo -n "        type BobOutput = struct { Num out_${m}_${n}_b" 
for ((i=1; i<=m-1; ++i)) ; do
	echo -n ", Num out_${i}_${n}_b"
done
for ((j=1; j<=n-1; ++j)) ; do
	echo -n ", Num out_${m}_${j}_b"
done

echo " };"

cat << END
        type Input = struct {AliceInput alice,  BobInput bob};
        type Output = struct {BobOutput bob};

        function Output output(Input input) {
	var Num xx;
	var Num tt;
END

for ((i=0; i<=m; ++i)) ; do
for ((j=0; j<=n; ++j)) ; do
cat << END
	var Num dd_${i}_${j};
END
done
done

for ((i=0; i<=m; ++i)) ; do
        echo "	dd_${i}_0 = input.alice.dd_${i}_0_a ^ input.bob.dd_${i}_0_b;"
done

for ((j=1; j<=n; ++j)) ; do
        echo "	dd_0_${j} = input.alice.dd_0_${j}_a ^ input.bob.dd_0_${j}_b;"
done

for ((i=1; i<=m; ++i)) ; do
for ((j=1; j<=n; ++j)) ; do
cat << END
	if (input.alice.x[$((i-1))] == input.bob.x[$((j-1))]) {
	  tt = dd_$((i-1))_$((j-1));
	} else {
	  tt = dd_$((i-1))_$((j-1)) + 1;
	}

	if (dd_$((i-1))_${j} < dd_${i}_$((j-1))) {
	  xx = dd_$((i-1))_${j} + 1;
	} else {
	  xx = dd_${i}_$((j-1)) + 1;
	}

	if (xx < tt) {
	  dd_${i}_${j} = xx;
	} else {
	  dd_${i}_${j} = tt;
	}
END
done
done

for ((i=1; i<=m; ++i)) ; do
	echo "	output.bob.out_${i}_${n}_b = dd_${i}_${n} ^ input.alice.out_${i}_${n}_a;"
done
for ((j=1; j<=n-1; ++j)) ; do
	echo "	output.bob.out_${m}_${j}_b = dd_${m}_${j} ^ input.alice.out_${m}_${j}_a;"
done

echo "	}"
echo "}"
