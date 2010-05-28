default:	thesis.pdf

clean:
	-rm -v thesis.[abilt][bdflou][cfglotx]
	find . -name \*.aux -exec rm -v "{}" \;

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


