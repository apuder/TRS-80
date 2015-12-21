#!/bin/sh

java -jar ../build/libs/tpk-all.jar \
    -a "Arno Puder" \
    -e mobile@puder.org \
    -n "Missile Defense" \
    -m 3 -d1 defense.cmd \
    -d "The only arcade game I ever wrote. Inspired by the Big Five games. Take a note of the date in the screenshot. This game can only be played with a game controller." \
    -s screenshot1.jpg screenshot2.jpg missile-defense.jpg

