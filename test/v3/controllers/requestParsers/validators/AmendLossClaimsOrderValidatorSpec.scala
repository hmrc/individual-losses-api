/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.models.errors._
import v3.models.requestData.AmendLossClaimsOrderRawData

class AmendLossClaimsOrderValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val invalidNino = "AA123456"
  private val validTaxYear = "2019-20"
  private val invalidTaxYearGap = "2018-20"
  private val invalidTaxYearFormat = "19-20"

  private val mtdRequest = Json.parse(
    """
      |{
      |  "claimType":"carry-sideways",
      |  "listOfLossClaims":[
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":1
      |     }
      |   ]
      |}
    """.stripMargin
  )

  private val mtdRequestCarryForward = Json.parse(
    """
      |{
      |  "claimType":"carry-forward",
      |  "listOfLossClaims":[
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":1
      |     }
      |   ]
      |}
    """.stripMargin
  )

  private val mtdRequestIdIncorrect = Json.parse(
    """
      |{
      |  "claimType":"carry-sideways",
      |  "listOfLossClaims":[
      |     {
      |       "id":"1234",
      |       "sequence":1
      |     }
      |   ]
      |}
    """.stripMargin
  )

  private val mtdRequestInvalidOrder = Json.parse(
    """
      |{
      |  "claimType":"carry-sideways",
      |  "listOfLossClaims":[
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":1
      |     },
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":3
      |     },
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":5
      |     }
      |   ]
      |}
    """.stripMargin
  )

  private val mtdRequestInvalidStart = Json.parse(
    """
      |{
      |  "claimType":"carry-sideways",
      |  "listOfLossClaims":[
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":3
      |     },
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":2
      |     },
      |     {
      |       "id":"AAZZ1234567890a",
      |       "sequence":4
      |     }
      |   ]
      |}
    """.stripMargin
  )

  private val mtdRequestInvalidBody = Json.parse(
    """
      |{
      |  "claimType":"carry-forward",
      |  "listOfLossClaims":[
      |     {
      |       "id":"walrus",
      |       "sequence":2
      |     },
      |     {
      |       "id":"walrus",
      |       "sequence":100
      |     }
      |   ]
      |}
    """.stripMargin
  )

  val validator = new AmendLossClaimsOrderValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(mtdRequest))) shouldBe Nil
      }
      "no Tax year is supplied" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, None, AnyContentAsJson(mtdRequest))) shouldBe Nil
      }
    }

    "return one error" when {
      "nino validation fails" in {
        validator.validate(AmendLossClaimsOrderRawData(invalidNino, Some(validTaxYear), AnyContentAsJson(mtdRequest))) shouldBe
          List(NinoFormatError)
      }

      "tax year gap is higher than 1" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(invalidTaxYearGap), AnyContentAsJson(mtdRequest))) shouldBe
          List(TaxYearFormatError)
      }

      "tax year format is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(invalidTaxYearFormat), AnyContentAsJson(mtdRequest))) shouldBe
          List(TaxYearFormatError)
      }

      "a non-carry-sideways claimType is provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(mtdRequestCarryForward))) shouldBe
          List(ClaimTypeFormatError)
      }

      "Id format is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(mtdRequestIdIncorrect))) shouldBe
          List(ClaimIdFormatError)
      }

      "a mandatory field isn't provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(Json.obj()))) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "sequence order is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(mtdRequestInvalidOrder))) shouldBe
          List(RuleSequenceOrderBroken)
      }
      "sequence start is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(mtdRequestInvalidStart))) shouldBe
          List(RuleInvalidSequenceStart)
      }
    }
    "return multiple errors" when {
      "invalid nino and tax year are provided" in {
        validator.validate(AmendLossClaimsOrderRawData("Walrus", Some("13900"), AnyContentAsJson(mtdRequest))) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
      "invalid body fields are provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, Some(validTaxYear), AnyContentAsJson(mtdRequestInvalidBody))) shouldBe
          List(RuleInvalidSequenceStart, RuleSequenceOrderBroken, ClaimTypeFormatError, ClaimIdFormatError, SequenceFormatError)
      }
    }
  }
}