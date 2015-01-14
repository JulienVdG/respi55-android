#!/bin/sh

# inkscape default dpi is 90
svg2android() {
s=$1
d=${s%%.svg}.png
inkscape $s --export-dpi=90 --export-png=../res/drawable-mdpi/$d
inkscape $s --export-dpi=135 --export-png=../res/drawable-hdpi/$d
inkscape $s --export-dpi=180 --export-png=../res/drawable-xhdpi/$d
inkscape $s --export-dpi=270 --export-png=../res/drawable-xxhdpi/$d
}

svg2android ic_stat_started.svg
svg2android ic_launcher.svg

#web icon
inkscape ic_launcher.svg --export-width=512 --export-height=512 --export-png=../ic_launcher-web.png
