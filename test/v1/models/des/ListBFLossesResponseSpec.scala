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
package v1.models.des

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.domain.{ BFLossId, BFLosses }

class ListBFLossesResponseSpec extends UnitSpec {

  "json reads" must {
    "work for des response" in {
      // Note we ignore all but the lossId currently...
      val desResponseJson = Json.parse("""
          |[
          |{
          |"incomeSourceId": "000000000000000",
          |"lossType": "INCOME",
          |"broughtForwardLossAmount": 99999999999.99,
          |"taxYear": "2000",
          |"lossId": "000000000000001",
          |"submissionDate": "2018-07-13T12:13:48.763Z"
          |},
          |{
          |"incomeSourceId": "000000000000000",
          |"lossType": "INCOME",
          |"broughtForwardLossAmount": 0.02,
          |"taxYear": "2000",
          |"lossId": "000000000000002",
          |"submissionDate": "2018-07-13T12:13:48.763Z"
          |},
          |{
          |"incomeSourceType": "02",
          |"broughtForwardLossAmount": 0.02,
          |"taxYear": "2000",
          |"lossId": "000000000000003",
          |"submissionDate": "2018-07-13T12:13:48.763Z"
          |},
          |  {
          |"incomeSourceType": "04",
          |"broughtForwardLossAmount": 0.02,
          |"taxYear": "2000",
          |"lossId": "000000000000004",
          |"submissionDate": "2018-07-13T12:13:48.763Z"
          |}
          |]
          |
        """.stripMargin)

      desResponseJson.as[ListBFLossesResponse] shouldBe
        ListBFLossesResponse(Seq("000000000000001", "000000000000002", "000000000000003", "000000000000004"))
    }
  }

  "conversion to mtd" must {
    "work" in {
      ListBFLossesResponse(Seq("000000000000001", "000000000000002")).toMtd shouldBe BFLosses(
        Seq(BFLossId("000000000000001"), BFLossId("000000000000002")))
    }
  }
}
