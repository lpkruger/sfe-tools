default:	thesis.pdf

TEXSOURCES=
LYXSOURCES=chapters/5cluster.tex chapters/3obdd.tex \
	chapters/2mywork.tex chapters/1background.tex chapters/0intro.tex

$(LYXSOURCES): proclyx

thesis.tex:	thesis.lyx proctex.pl $(LYXSOURCES) proclyx

proclyx:
	-rm thesis.tex
	lyx -e pdflatex thesis.lyx
	mv thesis.tex thesis_.tex
	cat header.tex > thesis.tex
	perl -n proctex.pl thesis_.tex >> thesis.tex
	perl -i -n -s proctex.pl -doprint $(LYXSOURCES)

viewpdf:	thesis.pdf
	acroread thesis.pdf

pdf:		thesis.pdf

thesis.aux:     thesis.tex
	pdflatex thesis.tex
	-bibtex thesis.aux
	pdflatex thesis.tex
	rm thesis.pdf

#depends := $(wildcard *.tex) $(wildcard */*.tex)
#$(info ${depends})
#$(info )
#thesis.pdf:     thesis.tex thesis.aux ${depends}

thesis.pdf:     thesis.tex thesis.aux $(LYXSOURCES) $(TEXSOURCES)
	pdflatex thesis.tex

clean:
	-rm -v thesis.[abilt][bdflou][cfglotx]
	find . -name \*.aux -exec rm -v "{}" \;

moreclean:      clean
	-rm thesis*.pdf
	-rm $(LYXSOURCES)
	-rm thesis.tex thesis_.tex

