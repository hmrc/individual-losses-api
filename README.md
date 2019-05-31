API Example Microservice
========================

This is an API example to test the API platform.
It has 3 endpoints, one for each level of authorisation (open, application-restricted and user-restricted).

## hello-world
This is an open resource (hence it doesn't require any type of authorisation)

request: 
```
GET /hello/world
```
response:
```
{
    "message":"Hello World"
}
```
In the API definition, `authType` should be set to `NONE`
```
"authType": "NONE"
 ```

## hello-application
This is an application-restricted resource (hence it requires a server token in the `Authorization` header)

request: 
```
GET /hello/application
```
response:
```
{
    "message":"Hello Application"
}
```
In the API definition, `authType` should be set to `APPLICATION`
```
"authType": "APPLICATION"
 ```

## hello-user
This is a user-restricted resource (hence it requires a user token in the `Authorization` header`)

request: 
```
GET /hello/user
```
response:
```
{
    "message":"Hello User"
}
```
In the API definition, `authType` should be `USER`
```
"authType": "USER"
```

# Sandbox
All endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint, e.g.
```
GET /sandbox/hello/world
GET /sandbox/hello/application
GET /sandbox/hello/user
```

# Definition
API definition for this service will be available under `/api/definition` endpoint.
See definition in `/resources/public/api/definition.json` for the format.

# Version
Version of API need to be provided in `Accept` request header
```
Accept: application/vnd.hmrc.v1.0+json or
Accept: application/vnd.hmrc.v2.0+json or
Accept: application/vnd.hmrc.v1.0+xml or
Accept: application/vnd.hmrc.v2.0+xml
```

# Run tests
```
sbt test
sbt it:test
sbt component:test
```

# License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
