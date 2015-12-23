#!/bin/sh

java -jar ../build/libs/tpk-all.jar \
    -a "Arno Puder" \
    -e mobile@puder.org \
    -n "Obstacle Run" \
    -m 3 -d1 disk-obstacle-run.cmd \
    -d "The only arcade game I ever wrote. Inspired by the Big Five games. Take a note of the date in the screenshot. This game can only be played with a game controller." \
    -s screenshot-obstacle-run-1.jpg screenshot-obstacle-run-2.jpg

java -jar ../build/libs/tpk-all.jar \
    -n "Eliminator" \
    -m 3 \
    -d1 disk-eliminator.dsk \
    -s screenshot-eliminator-1.png screenshot-eliminator-2.png

java -jar ../build/libs/tpk-all.jar \
    -n "Donkey Kong" \
    -m 3 \
    -d1 disk-donkey-kong.dsk \
    -s screenshot-donkey-kong-1.png screenshot-donkey-kong-2.png

java -jar ../build/libs/tpk-all.jar \
    -n "Sea Dragon" \
    -m 3 \
    -d1 disk-sea-dragon.dsk \
    -s screenshot-sea-dragon-1.png screenshot-sea-dragon-2.png

