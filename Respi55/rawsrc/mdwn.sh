#!/bin/sh
mkdir -p ../res/raw/ ../res/raw-fr/

markdown help.mdwn > ../res/raw/help.html
markdown help-fr.mdwn > ../res/raw-fr/help.html

markdown about.mdwn > ../res/raw/about.html
markdown about-fr.mdwn > ../res/raw-fr/about.html
