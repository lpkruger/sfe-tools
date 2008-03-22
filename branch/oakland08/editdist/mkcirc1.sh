m=$1
n=$2

cat << END
program edit_${m}_${n} {
        const N=8;
        type Num = Int<N>;
        type Char = Int<8>;
	type StrA = Char[$m];
	type StrB = Char[$n];
        type AliceInput = struct { StrA x };
        type BobInput = struct { StrB x };
        type BobOutput = Num; 
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
	echo "	dd_${i}_0 = ${i};"
done

for ((j=1; j<=n; ++j)) ; do
	echo "	dd_0_${j} = ${j};"
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

echo "	output.bob=dd_${m}_${n};"
echo "	}"
echo "}"
