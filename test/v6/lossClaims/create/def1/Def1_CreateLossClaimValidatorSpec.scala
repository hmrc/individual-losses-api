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

package v6.lossClaims.create.def1

import common.errors.{RuleTypeOfClaimInvalid, TaxYearClaimedForFormatError, TypeOfClaimFormatError, TypeOfLossFormatError}
import play.api.libs.json.{JsObject, JsValue, Json}
import shared.models.domain.Nino
import shared.models.errors.*
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v6.lossClaims.common.models.*
import v6.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v6.lossClaims.create.model.request.CreateLossClaimRequestData

class Def1_CreateLossClaimValidatorSpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino        = "AA123456A"
  private val invalidNino      = "badNino"
  private val validTaxYear     = "2019-20"
  private val validTypeOfLoss  = "self-employment"
  private val validTypeOfClaim = "carry-forward"
  private val validBusinessId  = "XAIS01234567890"

  private val parsedNino = Nino(validNino)

  private val emptyBody = JsObject.empty

  def requestBodyJson(typeOfLoss: String = validTypeOfLoss,
                      businessId: String = validBusinessId,
                      typeOfClaim: String = validTypeOfClaim,
                      taxYearClaimedFor: String = validTaxYear): JsValue = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "businessId" : "$businessId",
       |  "typeOfClaim" : "$typeOfClaim",
       |  "taxYearClaimedFor" : "$taxYearClaimedFor"
       |}
     """.stripMargin
  )

  private val validRequestBody = requestBodyJson()

  private val parsedRequestBody =
    Def1_CreateLossClaimRequestBody(validTaxYear, TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, validBusinessId)

  protected def validator(nino: String, body: JsValue) = new Def1_CreateLossClaimValidator(nino, body)

  "running a validation" should {

    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validator(validNino, validRequestBody).validateAndWrapResult()
        result shouldBe Right(
          Def1_CreateLossClaimRequestData(parsedNino, parsedRequestBody)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validRequestBody).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "given an empty JSON body" in {
        val result = validator(validNino, emptyBody).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }

      testMissingMandatory("taxYearClaimedFor")
      testMissingMandatory("typeOfLoss")
      testMissingMandatory("typeOfClaim")
      testMissingMandatory("businessId")

      def testMissingMandatory(field: String): Unit =
        s"a mandatory field $field is missing" in {
          val path   = s"/$field"
          val result = validator(validNino, requestBodyJson().removeProperty(path)).validateAndWrapResult()

          result shouldBe Left(
            ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath(path))
          )
        }
    }

    "return TaxYearClaimedForFormatError" when {
      "given an invalid tax year" in {
        val result = validator(validNino, requestBodyJson(taxYearClaimedFor = "2016")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearClaimedForFormatError.withPath("/taxYearClaimedFor"))
        )
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given a tax year with a range greater than a year" in {
        val result = validator(validNino, requestBodyJson(taxYearClaimedFor = "2019-21")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError.withPath("/taxYearClaimedFor"))
        )
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given an out of range tax year" in {
        val result = validator(validNino, requestBodyJson(taxYearClaimedFor = "2018-19")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError.withPath("/taxYearClaimedFor"))
        )
      }
    }

    "return TypeOfLossFormatError" when {
      "given an invalid loss type" in {
        val result = validator(validNino, requestBodyJson(typeOfLoss = "invalid")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfLossFormatError.withPath("/typeOfLoss"))
        )
      }
    }

    "return TypeOfClaimFormatError" when {
      "given an invalid claim type" in {
        val result = validator(validNino, requestBodyJson(typeOfClaim = "invalid")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfClaimFormatError.withPath("/typeOfClaim"))
        )
      }
    }

    "return BusinessIdFormatError" when {
      "given an invalid ID" in {
        val result = validator(validNino, requestBodyJson(businessId = "invalid")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BusinessIdFormatError)
        )
      }
    }

    "return RuleTypeOfClaimInvalid" when {
      "a typeOfClaim is not permitted with the typeOfLoss" in {
        val result = validator(
          validNino,
          requestBodyJson(typeOfLoss = "self-employment", typeOfClaim = "carry-forward-to-carry-sideways")
        ).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTypeOfClaimInvalid)
        )
      }

      "a typeOfLoss is self employment" when {
        permitOnly(TypeOfLoss.`self-employment`, List(TypeOfClaim.`carry-forward`, TypeOfClaim.`carry-sideways`))
      }

      "a typeOfLoss is uk-property" when {
        permitOnly(
          TypeOfLoss.`uk-property`,
          List(TypeOfClaim.`carry-sideways`, TypeOfClaim.`carry-sideways-fhl`, TypeOfClaim.`carry-forward-to-carry-sideways`))
      }

      "a typeOfLoss is foreign-property" when {
        permitOnly(
          TypeOfLoss.`foreign-property`,
          List(TypeOfClaim.`carry-sideways`, TypeOfClaim.`carry-sideways-fhl`, TypeOfClaim.`carry-forward-to-carry-sideways`))
      }

      def permitOnly(typeOfLoss: TypeOfLoss, permittedTypesOfClaim: Seq[TypeOfClaim]): Unit = {
        permittedTypesOfClaim.foreach(typeOfClaim =>
          s"permit $typeOfLoss with $typeOfClaim" in {
            val result = validator(
              validNino,
              requestBodyJson(typeOfLoss = typeOfLoss.toString, typeOfClaim = typeOfClaim.toString)
            ).validateAndWrapResult()

            result shouldBe Right(
              Def1_CreateLossClaimRequestData(parsedNino, parsedRequestBody.copy(typeOfLoss = typeOfLoss, typeOfClaim = typeOfClaim))
            )
          })

        TypeOfClaim.values
          .filterNot(permittedTypesOfClaim.contains)
          .foreach(typeOfClaim =>
            s"not permit $typeOfLoss with $typeOfClaim" in {
              val result = validator(
                validNino,
                requestBodyJson(typeOfLoss = typeOfLoss.toString, typeOfClaim = typeOfClaim.toString)
              ).validateAndWrapResult()

              result shouldBe Left(
                ErrorWrapper(correlationId, RuleTypeOfClaimInvalid)
              )
            })
      }

    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val requestBody =
          requestBodyJson(businessId = "invalid", taxYearClaimedFor = "2010-11")

        val result: Either[ErrorWrapper, CreateLossClaimRequestData] =
          validator(validNino, requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                BusinessIdFormatError,
                RuleTaxYearNotSupportedError.withPath("/taxYearClaimedFor")
              )))
        )
      }
    }

  }

}
