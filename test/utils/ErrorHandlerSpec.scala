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

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.mvc.Http.Status
import support.UnitSpec
import uk.gov.hmrc.auth.core.InvalidBearerToken

class ErrorHandlerSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  trait Setup {
    val errorHandler = app.injector.instanceOf[ErrorHandler]
  }

  "The Error Handler" should {

    "return bad request error in the case of an invalid json" in new Setup {
      val exception = new InvalidBearerToken()
      val result = await(errorHandler.onBadRequest(FakeRequest(), "Invalid Json"))

      result.header.status shouldBe Status.BAD_REQUEST
    }
  }
}
