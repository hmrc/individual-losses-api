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

import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.models.errors._
import v3.models.requestData.CreateLossClaimRawData
import v3.models.utils.JsonErrorValidators

class CreateLossClaimValidatorSpec extends UnitSpec with JsonErrorValidators {

  private val validNino        = "AA123456A"
  private val validTaxYear     = "2019-20"
  private val validTypeOfLoss  = "self-employment"
  private val validTypeOfClaim = "carry-forward"
  private val validBusinessId  = "XAIS01234567890"

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

  val validator = new CreateLossClaimValidator

  "running a validation" should {

    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson()))) shouldBe Nil
      }
    }

    "return IncorrectOrEmptyBodySubmitted error" when {
      "an incorrect or empty body is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(JsObject.empty))) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      testMissingMandatory("taxYearClaimedFor")
      testMissingMandatory("typeOfLoss")
      testMissingMandatory("typeOfClaim")
      testMissingMandatory("businessId")

      def testMissingMandatory(field: String): Unit =
        s"a mandatory field $field is missing" in {
          val path = s"/$field"
          validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson().removeProperty(path)))) shouldBe
            List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq(path))))
        }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(CreateLossClaimRawData("A12344A", AnyContentAsJson(requestBodyJson()))) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearClaimedForFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson(taxYearClaimedFor = "2016")))) shouldBe
          List(TaxYearClaimedForFormatError.copy(paths = Some(List("/taxYearClaimedFor"))))
      }
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson(taxYearClaimedFor = "2019-21")))) shouldBe
          List(RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYearClaimedFor"))))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson(taxYearClaimedFor = "2018-19")))) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossFormatError error" when {
      "an invalid loss type is submitted" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson(typeOfLoss = "invalid")))) shouldBe
          List(TypeOfLossFormatError)
      }
    }

    "return TypeOfClaimFormatError error" when {
      "an invalid claim type is submitted" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson(typeOfClaim = "invalid")))) shouldBe
          List(TypeOfClaimFormatError)
      }
    }

    "return BusinessIdFormatError error" when {
      "an invalid id is submitted" in {
        validator.validate(CreateLossClaimRawData(validNino, AnyContentAsJson(requestBodyJson(businessId = "invalid")))) shouldBe
          List(BusinessIdFormatError)
      }
    }

    "return RuleTypeOfClaimInvalid error" when {
      "a typeOfClaim is not permitted with the typeOfLoss" in {
        validator.validate(
          CreateLossClaimRawData(
            validNino,
            AnyContentAsJson(requestBodyJson(typeOfLoss = "self-employment", typeOfClaim = "carry-forward-to-carry-sideways")))) shouldBe
          List(RuleTypeOfClaimInvalid)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(
          CreateLossClaimRawData(
            validNino,
            AnyContentAsJson(requestBodyJson(typeOfLoss = "self-employment", businessId = "invalid", taxYearClaimedFor = "2010-11")))) shouldBe
          List(RuleTaxYearNotSupportedError, BusinessIdFormatError)
      }
    }
  }
}
