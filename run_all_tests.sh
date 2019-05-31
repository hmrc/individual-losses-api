#!/bin/bash

sbt clean coverage test it:test component:test coverageOff coverageReport
python dependencyReport.py api-example-microservice
