/*
 * Copyright 2019 HM Revenue & Customs
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

package utils

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.UnitSpec
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import v1.models.errors._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

class ErrorHandlerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  def versionHeader(version: String): (String, String) = ACCEPT -> s"application/vnd.hmrc.$version+json"

  class Test(versionInHeader: Option[String]) {
    val method = "some-method"

    val requestHeader = FakeRequest().withHeaders(versionInHeader.map(versionHeader).toSeq: _*)

    val auditConnector = MockitoSugar.mock[AuditConnector]
    val httpAuditEvent = MockitoSugar.mock[HttpAuditEvent]

    when(auditConnector.sendEvent(any[DataEvent]())(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.successful(Success))

    val configuration = Configuration("appName" -> "myApp")
    val handler = new ErrorHandler(configuration, auditConnector, httpAuditEvent)
  }

  "onClientError" should {

    Seq(Some("2.0"),
      Some("8.0"))
      .foreach(behaveAsVersion)

    def behaveAsVersion(versionInHeader: Option[String]): Unit =
      "return 404 with version 2 error body" when {
        s"version header is $versionInHeader" in new Test(versionInHeader) {

          val result = handler.onClientError(requestHeader, Status.NOT_FOUND, "test")
          status(result) shouldBe Status.NOT_FOUND

          contentAsJson(result) shouldBe Json.toJson(UnsupportedVersionError)
        }
      }

    "return 406 with error body" when {
      "no version header is supplied" in new Test(Some("XXX")) {
        val result = handler.onClientError(requestHeader, NOT_ACCEPTABLE, "test")
        status(result) shouldBe NOT_ACCEPTABLE

        contentAsJson(result) shouldBe Json.toJson(InvalidAcceptHeaderError)
      }

      "invalid version header is supplied" in new Test(None) {
        val result = handler.onClientError(requestHeader, NOT_ACCEPTABLE, "test")
        status(result) shouldBe NOT_ACCEPTABLE

        contentAsJson(result) shouldBe Json.toJson(InvalidAcceptHeaderError)
      }
    }

    "return 404 with version 1 error body" when {
      "resource not found and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onClientError(requestHeader, NOT_FOUND, "test")
        status(result) shouldBe NOT_FOUND

        contentAsJson(result) shouldBe Json.toJson(NotFoundError)
      }
    }

    "return 400 with version 1 error body" when {
      "JsValidationException thrown and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onClientError(requestHeader, BAD_REQUEST, "test")
        status(result) shouldBe BAD_REQUEST

        contentAsJson(result) shouldBe Json.toJson(BadRequestError)
      }
    }

    "return 401 with version 1 error body" when {
      "unauthorised and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onClientError(requestHeader, UNAUTHORIZED, "test")
        status(result) shouldBe UNAUTHORIZED

        contentAsJson(result) shouldBe Json.toJson(UnauthorisedError)
      }
    }

    "return 415 with version 1 error body" when {
      "unsupported body and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onClientError(requestHeader, UNSUPPORTED_MEDIA_TYPE, "test")
        status(result) shouldBe UNSUPPORTED_MEDIA_TYPE

        contentAsJson(result) shouldBe Json.toJson(InvalidBodyTypeError)
      }
    }

    "return 405 with version 1 error body" when {
      "invalid method type and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onClientError(requestHeader, METHOD_NOT_ALLOWED, "test")
        status(result) shouldBe METHOD_NOT_ALLOWED

        contentAsJson(result) shouldBe Json.toJson(MtdError("INVALID_REQUEST", "test"))
      }
    }
  }

  "onServerError" should {

    Seq(Some("2.0"),
      Some("8.0"),
      Some("XXX"))
      .foreach(behaveAsVersion)

    def behaveAsVersion(versionInHeader: Option[String]): Unit =
      "return 404 with version 2 error body" when {
        s"version header is $versionInHeader" in new Test(versionInHeader) {
          val resultF = handler.onServerError(requestHeader, new NotFoundException("test") with NoStackTrace)
          status(resultF) shouldEqual NOT_FOUND
          contentAsJson(resultF) shouldEqual Json.parse("""{"statusCode":404,"message":"test"}""")
        }
      }

    "return 404 with version 1 error body" when {
      "NotFoundException thrown and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onServerError(requestHeader, new NotFoundException("test") with NoStackTrace)
        status(result) shouldBe NOT_FOUND

        contentAsJson(result) shouldBe Json.toJson(NotFoundError)
      }
    }

    "return 401 with version 1 error body" when {
      "AuthorisationException thrown and version 1 header is supplied" in new Test(Some("1.0")) {
        val result = handler.onServerError(requestHeader, new InsufficientEnrolments("test") with NoStackTrace)
        status(result) shouldBe UNAUTHORIZED

        contentAsJson(result) shouldBe Json.toJson(UnauthorisedError)
      }

      "return 400 with version 1 error body" when {
        "JsValidationException thrown and version 1 header is supplied" in new Test(Some("1.0")) {
          val result = handler.onServerError(requestHeader, new JsValidationException("test", "test", classOf[String], "errs") with NoStackTrace)
          status(result) shouldBe BAD_REQUEST

          contentAsJson(result) shouldBe Json.toJson(BadRequestError)
        }
      }

      "return 500 with version 1 error body" when {
        "other exeption thrown and version 1 header is supplied" in new Test(Some("1.0")) {
          val result = handler.onServerError(requestHeader, new Exception with NoStackTrace)
          status(result) shouldBe INTERNAL_SERVER_ERROR

          contentAsJson(result) shouldBe Json.toJson(DownstreamError)
        }
      }
    }
  }
}

