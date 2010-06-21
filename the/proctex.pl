s{^\\section}{\\chapter};
s{^\\subsection}{\\section};
s{^\\subsubsection}{\\subsection};

if ($doprint) {
  print "$_";
}

$doprint = 1 if (/^\\begin\{document\}/);
