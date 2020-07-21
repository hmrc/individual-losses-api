/*
 * Copyright 2020 HM Revenue & Customs
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

  def createRequestBodyJson(typeOfLoss: String = validTypeOfLoss,
                            businessId: Option[String] = Some(validBusinessId),
                            typeOfClaim: String = validTypeOfClaim,
                            taxYear: String = validTaxYear
                           ): JsValue = Json.parse(
    businessId match {
      case Some(id) => s"""{
                          |  "typeOfLoss" : "$typeOfLoss",
                          |  "businessId" : "$id",
                          |  "typeOfClaim" : "$typeOfClaim",
                          |  "taxYear" : "$taxYear"
                          |}""".stripMargin

      case None => s"""{
                      |  "typeOfLoss" : "$typeOfLoss",
                      |  "typeOfClaim" : "$typeOfClaim",
                      |  "taxYear" : "$taxYear"
                      |}""".stripMargin
    }
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
            typeOfLoss = "uk-property-non-fhl", typeOfClaim = "carry-sideways", businessId = None)))) shouldBe Nil
      }

      "a valid property request with 'carry-sideways-fhl' is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino,
          AnyContentAsJson(createRequestBodyJson(typeOfLoss = "uk-property-non-fhl",
            typeOfClaim = "carry-sideways-fhl", businessId = None)))) shouldBe Nil
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
          List(TaxYearFormatError)
      }

      "a non-numeric tax year is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "XXXX-YY")))) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2019-21")))) shouldBe
          List(RuleTaxYearRangeInvalid)
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
          CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", businessId = None)))) shouldBe
          List(TypeOfLossFormatError)
      }

      "there is also a business id" in {
        validator.validate(
          CreateLossClaimRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", businessId = Some(validBusinessId))))) shouldBe
          List(TypeOfLossFormatError)
      }
    }

    "return BusinessIdValidation error" when {
      "an invalid id is submitted" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(createRequestBodyJson(businessId = Some("invalid"))))) shouldBe
          List(BusinessIdFormatError)
      }
    }

    "return RuleBusinessId error" when {
      "a business id is not provided for a self-employment loss type" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment", businessId = None)))) shouldBe
          List(RuleBusinessId)
      }
      "a business id is not provided for a foreign-property loss type" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "foreign-property", businessId = None)))) shouldBe
          List(RuleBusinessId)
      }
    }

    "return RuleTypeOfClaimInvalid error" when {
      "an incorrect typeOfClaim is used for self-employment typeOfLoss" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(
              typeOfLoss = "self-employment", typeOfClaim = "carry-forward-to-carry-sideways-general-income")))) shouldBe
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
              typeOfLoss = "foreign-property", typeOfClaim = "carry-forward-to-carry-sideways-general-income")))) shouldBe
          List(RuleTypeOfClaimInvalid)
      }
      "an incorrect typeOfClaim(carry-sideways-fhl) is used for foreign-property typeOfLoss" in {
        validator.validate(
          CreateLossClaimRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(
              typeOfLoss = "foreign-property", typeOfClaim = "carry-sideways-fhl")))) shouldBe
          List(RuleTypeOfClaimInvalid)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(
          CreateLossClaimRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment", businessId = None, taxYear = "2010-11")))) shouldBe
          List(RuleTaxYearNotSupportedError, RuleBusinessId)
      }
    }
  }
}
