#!/bin/sh

if [ -d var/googlecast-receiver ]; then
  cd var/googlecast-receiver
  rsync -aiv ./ apuder,trs80@web.sourceforge.net:/home/project-web/trs80/htdocs/
fi

