#!/bin/bash

sbt -J-Xmx4G -J-XX:+UseG1GC clean it:test