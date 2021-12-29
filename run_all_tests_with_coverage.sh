#!/bin/bash

sbt -J-Xmx4G -J-XX:+UseG1GC clean coverage test it:test coverageReport compile