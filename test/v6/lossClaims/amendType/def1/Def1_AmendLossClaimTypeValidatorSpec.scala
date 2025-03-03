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

package v6.lossClaims.amendType.def1

import common.errors.{ClaimIdFormatError, TypeOfClaimFormatError}
import play.api.libs.json.{JsValue, Json}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.utils.UnitSpec
import v6.lossClaims.amendType.def1.model.request.{Def1_AmendLossClaimTypeRequestBody, Def1_AmendLossClaimTypeRequestData}
import v6.lossClaims.common.models.{ClaimId, TypeOfClaim}

class Def1_AmendLossClaimTypeValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino      = "AA123456A"
  private val invalidNino    = "badNino"
  private val validClaimId   = "AAZZ1234567890a"
  private val invalidClaimId = "not-a-claim-id"
  private val validTaxYear   = "2019-20"

  private val parsedNino    = Nino(validNino)
  private val parsedClaimId = ClaimId(validClaimId)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)
  private val parsedBody    = Def1_AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  private def requestBodyJson(claimType: String = "carry-forward"): JsValue = Json.obj("typeOfClaim" -> claimType)
  private val validRequestBody: JsValue                                     = requestBodyJson()
  private val invalidRequestBody: JsValue                                   = Json.obj("wrong-field" -> "value")

  private def validator(nino: String, claimId: String, body: JsValue, taxYear: String) =
    new Def1_AmendLossClaimTypeValidator(nino, claimId, body, taxYear)

  "Amend Loss Claim Validator" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validator(validNino, validClaimId, validRequestBody, validTaxYear).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendLossClaimTypeRequestData(parsedNino, parsedClaimId, parsedBody, parsedTaxYear)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validClaimId, validRequestBody, validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return a single error" when {
      "given an invalid claimId" in {
        val result = validator(validNino, invalidClaimId, validRequestBody, validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ClaimIdFormatError)
        )
      }

      "given an empty JSON body" in {
        val result = validator(validNino, validClaimId, Json.obj(), validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }

      "given an invalid JSON body" in {
        val result = validator(validNino, validClaimId, invalidRequestBody, validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/typeOfClaim"))
        )
      }

      "given a JSON body with an invalid claim type" in {
        val result = validator(validNino, validClaimId, requestBodyJson("not-a-claim-type"), validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfClaimFormatError.withPath("/typeOfClaim"))
        )
      }

      "passed an incorrectly formatted taxYear" in {
        val result = validator(validNino, validClaimId, validRequestBody, "202324").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))

      }

      "passed a taxYear before the minimum supported" in {
        validator(validNino, validClaimId, validRequestBody, "2017-18").validateAndWrapResult() shouldBe
          Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed a taxYear spanning an invalid tax year range" in {
        val result = validator(validNino, validClaimId, validRequestBody, "2020-22").validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return multiple errors" when {
      "passed a request with multiple errors" in {
        val result = validator(invalidNino, invalidClaimId, invalidRequestBody, "invalidTaxYear").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                ClaimIdFormatError,
                NinoFormatError,
                TaxYearFormatError,
                RuleIncorrectOrEmptyBodyError.withPath("/typeOfClaim")
              )
            )
          )
        )
      }
    }
  }

}
