#!/bin/bash
for d in `cat dirs.sh`;
  do
        cp -r $d/target/mods/* $1
done
