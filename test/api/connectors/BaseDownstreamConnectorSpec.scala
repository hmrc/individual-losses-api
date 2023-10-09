/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package api.connectors

import api.connectors.DownstreamUri.{DesUri, IfsUri, TaxYearSpecificIfsUri}
import api.mocks.MockHttpClient
import api.models.outcomes.ResponseWrapper
import config.{AppConfig, MockAppConfig}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}

import scala.concurrent.Future

class BaseDownstreamConnectorSpec extends ConnectorSpec {
  val body        = "body"
  val outcome     = Right(ResponseWrapper(correlationId, Result(2)))
  val url         = "some/url?param=value"
  val absoluteUrl = s"$baseUrl/$url"

  // WLOG
  case class Result(value: Int)

  implicit val httpReads: HttpReads[DownstreamOutcome[Result]] = mock[HttpReads[DownstreamOutcome[Result]]]

  class DesLocalTest extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
    MockedAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)

    val qps = List("param1" -> "value1")
  }

  class IfsLocalTest extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockedAppConfig.ifsBaseUrl returns baseUrl
    MockedAppConfig.ifsToken returns "ifs-token"
    MockedAppConfig.ifsEnvironment returns "ifs-environment"
    MockedAppConfig.ifsEnvironmentHeaders returns Some(allowedIfsHeaders)

    val qps = List("param1" -> "value1")
  }

  class TysIfsLocalTest extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockedAppConfig.tysIfsBaseUrl returns baseUrl
    MockedAppConfig.tysIfsToken returns "TYS-IFS-token"
    MockedAppConfig.tysIfsEnvironment returns "TYS-IFS-environment"
    MockedAppConfig.tysIfsEnvironmentHeaders returns Some(allowedTysIfsHeaders)

    val qps = List("param1" -> "value1")
  }

  "for DES" when {
    "post" must {
      "posts with the required des headers and returns the result" in new DesLocalTest {
        implicit val hc: HeaderCarrier                    = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))
        val requiredDesHeadersPost: Seq[(String, String)] = requiredDesHeaders ++ List("Content-Type" -> "application/json")

        MockedHttpClient
          .post(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            body,
            requiredHeaders = requiredDesHeadersPost,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.post(body, DesUri[Result](url))) shouldBe outcome
      }
    }

    "get" must {
      "get with the required des headers and return the result" in new DesLocalTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))

        MockedHttpClient
          .get(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            parameters = qps,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.get(DesUri[Result](url), queryParams = qps)) shouldBe outcome
      }
    }

    "delete" must {
      "delete with the required des headers and return the result" in new DesLocalTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))

        MockedHttpClient
          .delete(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.delete(DesUri[Result](url))) shouldBe outcome
      }
    }

    "put" must {
      "put with the required des headers and return result" in new DesLocalTest {
        implicit val hc: HeaderCarrier                   = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))
        val requiredDesHeadersPut: Seq[(String, String)] = requiredDesHeaders ++ List("Content-Type" -> "application/json")

        MockedHttpClient
          .put(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            body,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.put(body, DesUri[Result](url))) shouldBe outcome
      }
    }

    "content-type header already present and set to be passed through" must {
      "override (not duplicate) the value" when {
        testNoDuplicatedContentType("Content-Type" -> "application/user-type")
        testNoDuplicatedContentType("content-type" -> "application/user-type")

        def testNoDuplicatedContentType(userContentType: (String, String)): Unit =
          s"for user content type header $userContentType" in new DesLocalTest {
            implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List(userContentType))

            MockedHttpClient
              .put(
                absoluteUrl,
                config = dummyHeaderCarrierConfig,
                body,
                requiredHeaders = requiredDesHeaders ++ List("Content-Type" -> "application/json"),
                excludedHeaders = List(userContentType)
              )
              .returns(Future.successful(outcome))

            await(connector.put(body, DesUri[Result](url))) shouldBe outcome
          }
      }
    }
  }

  "for IFS" when {
    "post" must {
      "posts with the required ifs headers and returns the result" in new IfsLocalTest {
        implicit val hc: HeaderCarrier                    = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))
        val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfsHeaders ++ List("Content-Type" -> "application/json")

        MockedHttpClient
          .post(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            body,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.post(body, IfsUri[Result](url))) shouldBe outcome
      }
    }

    "get" must {
      "get with the required des headers and return the result" in new IfsLocalTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))

        MockedHttpClient
          .get(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            parameters = qps,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.get(IfsUri[Result](url), queryParams = qps)) shouldBe outcome
      }
    }

    "delete" must {
      "delete with the required des headers and return the result" in new IfsLocalTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))

        MockedHttpClient
          .delete(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.delete(IfsUri[Result](url))) shouldBe outcome
      }
    }

    "put" must {
      "put with the required des headers and return result" in new IfsLocalTest {
        implicit val hc: HeaderCarrier                   = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))
        val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfsHeaders ++ List("Content-Type" -> "application/json")

        MockedHttpClient
          .put(
            absoluteUrl,
            config = dummyHeaderCarrierConfig,
            body,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.put(body, IfsUri[Result](url))) shouldBe outcome
      }
    }

    "content-type header already present and set to be passed through" must {
      "override (not duplicate) the value" when {
        testNoDuplicatedContentType("Content-Type" -> "application/user-type")
        testNoDuplicatedContentType("content-type" -> "application/user-type")

        def testNoDuplicatedContentType(userContentType: (String, String)): Unit =
          s"for user content type header $userContentType" in new IfsLocalTest {
            implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List(userContentType))

            MockedHttpClient
              .put(
                absoluteUrl,
                config = dummyHeaderCarrierConfig,
                body,
                requiredHeaders = requiredIfsHeaders ++ List("Content-Type" -> "application/json"),
                excludedHeaders = List(userContentType)
              )
              .returns(Future.successful(outcome))

            await(connector.put(body, IfsUri[Result](url))) shouldBe outcome
          }
      }
    }

    "for TYS-IFS" when {
      "post" must {
        "posts with the required ifs headers and returns the result" in new TysIfsLocalTest {
          implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))
          val requiredTysIfsHeadersPost: Seq[(String, String)] = requiredTysIfsHeaders ++ List("Content-Type" -> "application/json")

          MockedHttpClient
            .post(
              absoluteUrl,
              config = dummyHeaderCarrierConfig,
              body,
              requiredHeaders = requiredTysIfsHeadersPost,
              excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
            .returns(Future.successful(outcome))

          await(connector.post(body, TaxYearSpecificIfsUri[Result](url))) shouldBe outcome
        }
      }

      "get" must {
        "get with the required des headers and return the result" in new TysIfsLocalTest {
          implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))

          MockedHttpClient
            .get(
              absoluteUrl,
              config = dummyHeaderCarrierConfig,
              parameters = qps,
              requiredHeaders = requiredTysIfsHeaders,
              excludedHeaders = List("AnotherHeader" -> "HeaderValue")
            )
            .returns(Future.successful(outcome))

          await(connector.get(TaxYearSpecificIfsUri[Result](url), queryParams = qps)) shouldBe outcome
        }
      }

      "delete" must {
        "delete with the required des headers and return the result" in new TysIfsLocalTest {
          implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))

          MockedHttpClient
            .delete(
              absoluteUrl,
              config = dummyHeaderCarrierConfig,
              requiredHeaders = requiredTysIfsHeaders,
              excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
            .returns(Future.successful(outcome))

          await(connector.delete(TaxYearSpecificIfsUri[Result](url))) shouldBe outcome
        }
      }

      "put" must {
        "put with the required des headers and return result" in new TysIfsLocalTest {
          implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List("Content-Type" -> "application/json"))
          val requiredTysIfsHeadersPut: Seq[(String, String)] = requiredTysIfsHeaders ++ List("Content-Type" -> "application/json")

          MockedHttpClient
            .put(
              absoluteUrl,
              config = dummyHeaderCarrierConfig,
              body,
              requiredHeaders = requiredTysIfsHeadersPut,
              excludedHeaders = List("AnotherHeader" -> "HeaderValue"))
            .returns(Future.successful(outcome))

          await(connector.put(body, TaxYearSpecificIfsUri[Result](url))) shouldBe outcome
        }
      }

      "content-type header already present and set to be passed through" must {
        "override (not duplicate) the value" when {
          testNoDuplicatedContentType("Content-Type" -> "application/user-type")
          testNoDuplicatedContentType("content-type" -> "application/user-type")

          def testNoDuplicatedContentType(userContentType: (String, String)): Unit =
            s"for user content type header $userContentType" in new TysIfsLocalTest {
              implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ List(userContentType))

              MockedHttpClient
                .put(
                  absoluteUrl,
                  config = dummyHeaderCarrierConfig,
                  body,
                  requiredHeaders = requiredTysIfsHeaders ++ List("Content-Type" -> "application/json"),
                  excludedHeaders = List(userContentType)
                )
                .returns(Future.successful(outcome))

              await(connector.put(body, TaxYearSpecificIfsUri[Result](url))) shouldBe outcome
            }
        }
      }
    }
  }

}
