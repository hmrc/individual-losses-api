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

package api.models.errors

import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.Json
import support.UnitSpec

class MtdErrorSpec extends UnitSpec {

  val dummyError: MtdError = MtdError("SOME_CODE", "some message", BAD_REQUEST)

  "writes" should {
    "generate the correct JSON" in {
      Json.toJson(dummyError) shouldBe Json.parse(
        """
          |{
          |   "code": "SOME_CODE",
          |   "message": "some message"
          |}
        """.stripMargin
      )
    }
  }

  "withExtraPath" when {
    "paths are undefined" should {
      "create a new error with paths" in {
        dummyError.withExtraPath("aPath") shouldBe dummyError.copy(paths = Some(Seq("aPath")))
      }
    }

    "paths are defined" should {
      "add the new path to the existing list of paths" in {
        val dummyErrorWithPaths: MtdError = dummyError.copy(paths = Some(Seq("aPath")))

        dummyErrorWithPaths.withExtraPath("aPath2") shouldBe
          dummyErrorWithPaths.copy(paths = Some(Seq("aPath", "aPath2")))
      }
    }
  }

}
