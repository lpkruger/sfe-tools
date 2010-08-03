default:	thesis.pdf

clean:
	-rm -v thesis.[abilt][bdflou][cfglotx]
	find . -name \*.aux -exec rm -v "{}" \;

moreclean:	clean
	rm thesis*.pdf

thesis.aux:	thesis.tex
	pdflatex thesis.tex
	-bibtex thesis.aux
	pdflatex thesis.tex
	rm thesis.pdf

depends := $(wildcard *.tex) $(wildcard */*.tex)
$(info ${depends})
$(info )

thesis.pdf:	thesis.tex thesis.aux ${depends}
	pdflatex thesis.tex

booklet:	thesis.pdf
	sh splitpdf.sh thesis.pdf

viewpdf:	thesis.pdf
	acroread thesis.pdf

pdf:	clean thesis.pdf
