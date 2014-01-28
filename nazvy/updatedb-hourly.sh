#!/bin/bash
set -x
cd `dirname $0`

PATH=../osmutils:$PATH
## Зьмены за апошнія 2 гадзіны
osmupdate --hour --keep-tempfiles NOW-7800 tmp/hourly.osc.gz
../osmosis/bin/osmosis --read-xml-change file=tmp/hourly.osc.gz --write-pgsql-change host=localhost database=osm user=osm

psql --dbname=osm --user osm --file=scripts/after_update.sql || exit 1