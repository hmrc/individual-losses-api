individual-losses-api
========================

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The Individual Losses API allows a developer to show and provide a taxpayers’ financial data for their Brought Forward
Losses and Loss Claims.

For Brought Forward Losses, a developer can:

- provide a list of brought forward losses
- create a new brought forward loss
- show a single brought forward loss
- delete an existing brought forward loss
- update an existing brought forward loss

For Loss Claims, a developer can:

- provide a list of loss claims
- create a loss claim against an income source for a specific tax year
- show the detail of an existing loss claim
- delete a previously entered loss claim
- update a previously entered loss claim

## Requirements

- Scala 2.13.x
- Java 11
- sbt 1.9.x
- [Service Manager V2](https://github.com/hmrc/sm2)

## Development Setup

Run the microservice from the console using: `sbt run` (starts on port 9779 by default)

Start the service manager profile: `sm2 --start MTDFB_LOSSES`

## Run Tests

Run unit tests: `sbt test`

Run integration tests: `sbt it/test`

## To view the OAS

To view documentation locally, ensure the Individual Losses API is running, and run api-documentation-frontend:

```
./run_local_with_dependencies.sh
```

Then go to http://localhost:9680/api-documentation/docs/openapi/preview and use the appropriate port and version:

```
http://localhost:9779/api/conf/6.0/application.yaml
```

## Changelog

You can see our changelog [here](https://github.com/hmrc/income-tax-mtd-changelog/wiki)

## Support and Reporting Issues

You can create a GitHub issue [here](https://github.com/hmrc/income-tax-mtd-changelog/issues)

## API Reference / Documentation

Available on
the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-losses-api)

## License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")