#!/bin/bash

for arch in arm x86 mips
do
  for arg in pie nopie static
  do
    ./compile-busybox.sh $arch $arg
  done
done
