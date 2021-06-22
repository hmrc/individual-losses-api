individual-losses-api
========================

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The Individual Losses API allows a developer to show and provide a taxpayers’ financial data for their Brought Forward Losses and Loss Claims.

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
- Scala 2.12.x
- Java 8
- sbt 1.3.13
- [Service Manager](https://github.com/hmrc/service-manager)

## Development Setup
Run the microservice from the console using: `sbt run` (starts on port 9779 by default)

Start the service manager profile: `sm --start MTDFB_LOSSES`
 
## Run Tests
Run unit tests: `sbt test`

Run integration tests: `sbt it:test`

## To view the RAML
To view documentation locally, ensure the Individual Losses API is running, and run api-documentation-frontend:

```
./run_local_with_dependencies.sh
```

Then go to http://localhost:9680/api-documentation/docs/preview and enter the full URL path to the RAML file with the appropriate port and version:

```
http://localhost:9779/api/conf/2.0/application.raml
```

## Changelog

You can see our changelog [here](https://github.com/hmrc/income-tax-mtd-changelog/wiki)

## Support and Reporting Issues

You can create a GitHub issue [here](https://github.com/hmrc/income-tax-mtd-changelog/issues)

## API Reference / Documentation 
Available on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individual-losses-api/2.0)

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")