/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.controllers.requestParsers.validators

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.models.domain.TypeOfLoss
import v2.models.errors._
import v2.models.requestData.CreateLossClaimRawData

class CreateLossClaimValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2019-20"
  private val validTypeOfLoss = "self-employment"
  private val validTypeOfClaim = "carry-forward"
  private val validBusinessId = "XAIS01234567890"

  val emptyBody: JsValue = Json.parse(
    s"""{
       |
       |
       |}""".stripMargin
  )

  val requestBodyJsonNoId = Json.parse(
    s"""{
      |  "typeOfLoss" : "$validTypeOfLoss",
      |  "typeOfClaim" : "$validTypeOfClaim",
      |  "taxYear" : "$validTaxYear"
      |}""".stripMargin
  )

  def createRequestBodyJson(typeOfLoss: String = validTypeOfLoss,
                            businessId: String = validBusinessId,
                            typeOfClaim: String = validTypeOfClaim,
                            taxYear: String = validTaxYear
                           ): JsValue = Json.parse(
    s"""{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "businessId" : "$businessId",
       |  "typeOfClaim" : "$typeOfClaim",
       |  "taxYear" : "$taxYear"
       |}""".stripMargin
  )

  def createRequestUkPropertyNonFhlBodyJson(typeOfLoss: String = validTypeOfLoss,
                                            typeOfClaim: String = validTypeOfClaim,
                                            taxYear: String = validTaxYear
                                           ): JsValue = Json.parse(
    s"""{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "typeOfClaim" : "$typeOfClaim",
       |  "taxYear" : "$taxYear"
       |}""".stripMargin
  )

  val validator = new CreateLossClaimValidator

  "running a validation" should {

    "return no errors" when {

      "a valid employment request is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson()))) shouldBe Nil
      }

      "a valid property request is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino,
          AnyContentAsJson(createRequestBodyJson(
            typeOfLoss = "self-employment", typeOfClaim = "carry-sideways")))) shouldBe Nil
      }

      "a valid property request with 'carry-sideways-fhl' is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino,
          AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment",
            typeOfClaim = "carry-sideways")))) shouldBe Nil
      }
    }


    "return IncorrectOrEmptyBodySubmitted error" when {
      "an incorrect or empty body is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(emptyBody))) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(CreateLossClaimRawData("A12344A", AnyContentAsJson(createRequestBodyJson()))) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2016")))) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxYear"))))
      }

      "a non-numeric tax year is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "XXXX-YY")))) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxYear"))))
      }
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2019-21")))) shouldBe
          List(RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYear"))))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2018-19")))) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossValidation error" when {
      "an invalid loss type is submitted" in {
        validator.validate(
          CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", businessId = validBusinessId)))) shouldBe
          List(TypeOfLossFormatError)
      }

      "there is also a business id" in {
        validator.validate(
          CreateLossClaimRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", businessId = validBusinessId)))) shouldBe
          List(TypeOfLossFormatError)
      }
    }

    "return RuleBusinessId error" when {
      Seq[TypeOfLoss](
        TypeOfLoss.`uk-property-non-fhl`
      ).foreach {
        typeOfLoss => s"passed in $typeOfLoss with a valid businessId" in {
          validator.validate(
            CreateLossClaimRawData(
              validNino,
              AnyContentAsJson(Json.parse(
                s"""{
                   |  "typeOfLoss" : "$typeOfLoss",
                   |  "businessId" : "$validBusinessId",
                   |  "typeOfClaim" : "$validTypeOfClaim",
                   |  "taxYear" : "$validTaxYear"
                   |}""".stripMargin))
            )
          ) shouldBe List(RuleBusinessId)
        }
      }
      Seq[TypeOfLoss](
        TypeOfLoss.`self-employment`,
        TypeOfLoss.`foreign-property`
      ).foreach {
        typeOfLoss => s"passed in $typeOfLoss with no businessId" in {
          validator.validate(
            CreateLossClaimRawData(
              validNino,
              AnyContentAsJson(Json.parse(
                s"""{
                   |  "typeOfLoss" : "$typeOfLoss",
                   |  "typeOfClaim" : "$validTypeOfClaim",
                   |  "taxYear" : "$validTaxYear"
                   |}""".stripMargin))
            )
          ) shouldBe List(RuleBusinessId)
        }
      }
    }

    "return BusinessIdValidation error" when {
      "an invalid id is submitted" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(businessId = "invalid")))) shouldBe
          List(BusinessIdFormatError)
      }
    }


    "return RuleTypeOfClaimInvalid error" when {
      "an incorrect typeOfClaim is used for self-employment typeOfLoss" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(
              typeOfLoss = "self-employment", typeOfClaim = "carry-forward-to-carry-sideways")))) shouldBe
          List(RuleTypeOfClaimInvalid)
      }
      "an incorrect typeOfClaim(carry-sideways-fhl) is used for self-employment typeOfLoss" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(
              typeOfLoss = "self-employment", typeOfClaim = "carry-sideways-fhl")))) shouldBe
          List(RuleTypeOfClaimInvalid)
      }
      "an incorrect typeOfClaim is used for foreign-property typeOfLoss" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(
              typeOfLoss = "foreign-property", typeOfClaim = "carry-forward")))) shouldBe
          List(RuleTypeOfClaimInvalid)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(
          CreateLossClaimRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment", businessId = "invalid", taxYear = "2010-11")))) shouldBe
          List(RuleTaxYearNotSupportedError, BusinessIdFormatError)
      }
    }
  }
}
