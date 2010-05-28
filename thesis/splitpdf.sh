#set -x

bookn=32
in="$1"

#layout="${bookn}r,1r,2l,$((bookn-1))l"
layout="${bookn}r,1r,$((bookn-1))l,2l"
for ((i=3; i<=bookn/2; i=i+2)) ; do
#  layout=$layout,$((bookn-i+1))r,${i}r,$((i+1))l,$((bookn-i))l
  layout=$layout,$((bookn-i+1))r,${i}r,$((bookn-i))l,$((i+1))l
done

echo $layout

acro="acroread"
pages=$(pdfInfo "$in" | grep "^Page count:" | sed -e 's/Page count: //')
for ((i=1; i<=pages; i+=bookn)) ; do
  j=$(( i + bookn - 1))
  if (( j > pages )) ; then
    j=$pages
  fi
  ii=$(printf %04d $i)
  out="${in%.pdf}"-$ii.pdf
  pdfSplit -pages $i-$j "$in"
  mv "${in%.pdf}"-x.pdf "$out" 
#  acro="$acro $out"

  pdfImpose -verbose -dim 1x2 -layout $layout "$out"
  mv "${out%.pdf}-up.pdf" "$out"
done

#eval $acro &
