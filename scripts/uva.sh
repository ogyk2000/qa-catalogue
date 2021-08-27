#!/usr/bin/env bash

. ./setdir.sh
NAME=uva
MARC_DIR=${BASE_INPUT_DIR}/uva/2021-07-07
TYPE_PARAMS="--marcxml --marcVersion UVA --emptyLargeCollectors"
MASK=uva_*.xml

. ./common-script

if [[ "$1" != "help" ]]; then
  echo "DONE"
fi

exit 0