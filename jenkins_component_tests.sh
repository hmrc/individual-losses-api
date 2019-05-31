#!/bin/bash

function random_port {
    python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'
}

java -version

cd $WORKSPACE

shopt -s expand_aliases

export WIREMOCK_SERVICE_LOCATOR_PORT=`random_port`
echo "Running component test for service locator with wiremock on ${WIREMOCK_SERVICE_LOCATOR_PORT}"
sbt it:test

export WIREMOCK_PORT=`random_port`
export MICROSERVICE_PORT=`random_port`

echo "Starting MICROSERVICE on ${MICROSERVICE_PORT}"
echo "With wiremock on ${WIREMOCK_PORT}"

sbt stage 
target/universal/stage/bin/api-microservice -Drun.mode=Stub -Dhttp.port=${MICROSERVICE_PORT} 1> stdout.log 2> stderr.log &

while ! lsof -t -i:${MICROSERVICE_PORT} ; do sleep 5; done

export HOST="http://localhost:${MICROSERVICE_PORT}"

export DISPLAY=${DISPLAY=":99"}
sbt component:test

EXIT_CODE=$?
kill -9 `lsof -t -i:${MICROSERVICE_PORT}`
exit $EXIT_CODE

