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

package api.endpoints.lossClaim.amendOrder.v3.request

import api.models.errors._
import api.models.utils.JsonErrorValidators
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.mvc.AnyContentAsJson
import support.UnitSpec

class AmendLossClaimsOrderValidatorSpec extends UnitSpec with JsonErrorValidators {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2019-20"

  private def item(seq: Int, claimId: String = "AAZZ1234567890a") = Json.parse(s"""
       |{
       |  "claimId":"$claimId",
       |  "sequence": $seq
       |}
    """.stripMargin)

  private def mtdRequestWith(typeOfClaim: String = "carry-sideways", items: Seq[JsValue]) =
    Json.parse(s"""
      |{
      |
      |  "typeOfClaim":"$typeOfClaim",
      |  "listOfLossClaims": ${JsArray(items)}
      |}
    """.stripMargin)

  private val mtdRequest = mtdRequestWith(items = Seq(item(1)))

  val validator = new AmendLossClaimsOrderValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(mtdRequest))) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "nino validation fails" in {
        validator.validate(AmendLossClaimsOrderRawData("badNino", validTaxYear, AnyContentAsJson(mtdRequest))) shouldBe
          List(NinoFormatError)
      }
    }

    "return RuleTaxYearRangeInvalid" when {
      "tax year gap is higher than 1" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, "2020-22", AnyContentAsJson(mtdRequest))) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return TaxYearFormatError" when {
      "tax year format is invalid" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, "badTaxYear", AnyContentAsJson(mtdRequest))) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "tax year is too early" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, "2018-19", AnyContentAsJson(mtdRequest))) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfClaimFormatError" when {
      "a non-carry-sideways typeOfClaim is provided" in {
        val requestBody = mtdRequestWith(typeOfClaim = "carry-forward", items = Seq(item(1)))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(TypeOfClaimFormatError)
      }

      "a bad typeOfClaim is provided" in {
        val requestBody = mtdRequestWith(typeOfClaim = "invalid-type", items = Seq(item(1)))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(TypeOfClaimFormatError)
      }
    }

    "return ClaimIdFormatError" when {
      "claimId format is invalid" in {
        val requestBody = mtdRequestWith(items = Seq(item(1), item(seq = 2, claimId = "badValue")))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(ClaimIdFormatError.copy(paths = Some(Seq("/listOfLossClaims/1/claimId"))))
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "empty json is provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(Json.obj()))) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "the typeOfClaim field isn't provided" in {
        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(mtdRequest.removeProperty("typeOfClaim")))) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/typeOfClaim"))))
      }

      "the claim id isn't provided" in {
        val requestBody = mtdRequestWith(items = Seq(item(1).removeProperty("claimId")))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/listOfLossClaims/0/claimId"))))
      }

      "the sequence number isn't provided" in {
        val requestBody = mtdRequestWith(items = Seq(item(1).removeProperty("sequence")))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/listOfLossClaims/0/sequence"))))
      }
    }

    "return RuleSequenceOrderBroken" when {
      "sequence is broken" in {
        val requestBody = mtdRequestWith(items = Seq(item(1), item(3)))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(RuleSequenceOrderBroken)
      }
    }

    "return RuleInvalidSequenceStart" when {
      "sequence start is invalid" in {
        val requestBody = mtdRequestWith(items = Seq(item(seq = 2)))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(RuleInvalidSequenceStart)
      }
    }

    "return SequenceFormatError" when {
      "sequence number is out of range" in {
        val requestBody = mtdRequestWith(items = (1 to 100).map(i => item(seq = i)))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(ValueFormatError.forPathAndRange("/listOfLossClaims/99/sequence", "1", "99"))
      }
    }

    "return multiple errors" when {
      "invalid nino and tax year are provided" in {
        validator.validate(AmendLossClaimsOrderRawData("invalid", "13900", AnyContentAsJson(mtdRequest))) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }

      "invalid body fields are provided" in {
        val requestBody = mtdRequestWith(items = Seq(item(seq = 2, claimId = "bad"), item(1000)))

        validator.validate(AmendLossClaimsOrderRawData(validNino, validTaxYear, AnyContentAsJson(requestBody))) shouldBe
          List(
            RuleInvalidSequenceStart,
            RuleSequenceOrderBroken,
            ClaimIdFormatError.copy(paths = Some(Seq("/listOfLossClaims/0/claimId"))),
            ValueFormatError.forPathAndRange("/listOfLossClaims/1/sequence", "1", "99")
          )
      }
    }
  }
}
