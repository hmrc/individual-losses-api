/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.amendOrder.def1

import common.errors.{ClaimIdFormatError, RuleInvalidSequenceStart, RuleSequenceOrderBroken, TaxYearClaimedForFormatError, TypeOfClaimFormatError}
import play.api.libs.json.{JsArray, JsValue, Json}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors.*
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v6.lossClaims.amendOrder.def1.model.request.{Claim, Def1_AmendLossClaimsOrderRequestBody, Def1_AmendLossClaimsOrderRequestData}
import v6.lossClaims.common.models.TypeOfClaim

class Def1_AmendLossClaimsOrderValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val invalidNino  = "badNino"
  private val validTaxYear = "2019-20"

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

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

  private val parsedBody: Def1_AmendLossClaimsOrderRequestBody =
    Def1_AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, List(Claim("AAZZ1234567890a", 1)))

  private val mtdRequest = mtdRequestWith(items = List(item(1)))

  private def validator(nino: String, taxYearClaimedFor: String, body: JsValue) =
    new Def1_AmendLossClaimsOrderValidator(nino, taxYearClaimedFor, body)

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validator(validNino, validTaxYear, mtdRequest).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendLossClaimsOrderRequestData(parsedNino, parsedTaxYear, parsedBody)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validTaxYear, mtdRequest).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return RuleTaxYearRangeInvalid" when {
      "tax year gap is higher than 1" in {
        val result = validator(validNino, "2020-22", mtdRequest).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }
    }

    "return TaxYearFormatError" when {
      "tax year format is invalid" in {
        val result = validator(validNino, "2020-XY", mtdRequest).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearClaimedForFormatError)
        )
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "tax year is too early" in {
        val result = validator(validNino, "2018-19", mtdRequest).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }
    }

    "return TypeOfClaimFormatError" when {
      "given a non-carry-sideways typeOfClaim" in {
        val requestBody = mtdRequestWith(typeOfClaim = "carry-forward", items = List(item(1)))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfClaimFormatError)
        )
      }

      "given an invalid typeOfClaim" in {
        val requestBody = mtdRequestWith(typeOfClaim = "invalid-type", items = List(item(1)))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfClaimFormatError)
        )
      }
    }

    "return ClaimIdFormatError" when {
      "claimId format is invalid" in {
        val requestBody = mtdRequestWith(items = List(item(1), item(seq = 2, claimId = "badValue")))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, ClaimIdFormatError.withPath("/listOfLossClaims/1/claimId"))
        )
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "given an empty JSON body" in {
        val result = validator(validNino, validTaxYear, Json.obj()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }

      "given a body without a typeOfClaim field" in {
        val requestBody = mtdRequest.removeProperty("typeOfClaim")
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/typeOfClaim"))
        )
      }

      "given a body with a missing claim ID" in {
        val requestBody = mtdRequestWith(items = List(item(1).removeProperty("claimId")))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/listOfLossClaims/0/claimId"))
        )
      }

      "given a body with a missing sequence number" in {
        val requestBody = mtdRequestWith(items = List(item(1).removeProperty("sequence")))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/listOfLossClaims/0/sequence"))
        )
      }
    }

    "return RuleSequenceOrderBroken" when {
      "the sequence is broken" in {
        val requestBody = mtdRequestWith(items = List(item(1), item(3)))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleSequenceOrderBroken)
        )
      }
    }

    "return RuleInvalidSequenceStart" when {
      "the sequence start is invalid" in {
        val requestBody = mtdRequestWith(items = List(item(seq = 2)))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleInvalidSequenceStart)
        )
      }
    }

    "return SequenceFormatError" when {
      "the sequence number is out of range" in {
        val requestBody = mtdRequestWith(items = (1 to 100).map(i => item(seq = i)))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.forPathAndRange("/listOfLossClaims/99/sequence", "1", "99"))
        )
      }
    }

    "return multiple errors" when {
      "invalid nino and tax year are provided" in {
        val result = validator(invalidNino, "13900", mtdRequest).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearClaimedForFormatError)))
        )
      }

      "invalid body fields are provided" in {
        val requestBody = mtdRequestWith(items = List(item(seq = 2, claimId = "bad"), item(1000)))
        val result      = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(
              ClaimIdFormatError.withPath("/listOfLossClaims/0/claimId"),
              ValueFormatError.forPathAndRange("/listOfLossClaims/1/sequence", "1", "99"),
              RuleInvalidSequenceStart,
              RuleSequenceOrderBroken
            ))
          )
        )
      }
    }
  }

}
