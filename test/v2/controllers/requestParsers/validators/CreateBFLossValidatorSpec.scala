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

package v2.controllers.requestParsers.validators

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.models.domain.TypeOfLoss
import v2.models.errors._
import v2.models.requestData
import v2.models.requestData.CreateBFLossRawData

class CreateBFLossValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"
  private val validTypeOfLoss = "self-employment"
  private val validBusinessId = "XAIS01234567890"

  val emptyBody: JsValue = Json.parse(
    s"""{}""".stripMargin
  )

  def createRequestBodyJson(typeOfLoss: String = validTypeOfLoss,
                            businessId: String = validBusinessId,
                            taxYear: String = validTaxYear,
                            lossAmount: BigDecimal = 1000): JsValue = Json.parse(
    s"""{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "businessId" : "$businessId",
       |  "taxYear" : "$taxYear",
       |  "lossAmount" : $lossAmount
       |}""".stripMargin
  )

  val validator = new CreateBFLossValidator

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson()))) shouldBe Nil
      }
    }

    "return IncorrectOrEmptyBodySubmitted error" when {
      "an incorrect or empty body is supplied" in {
        validator.validate(CreateBFLossRawData(validNino, AnyContentAsJson(emptyBody))) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(requestData.CreateBFLossRawData("A12344A", AnyContentAsJson(createRequestBodyJson()))) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2016")))) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxYear"))))
      }

      "a non-numeric tax year is supplied" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "XXXX-YY")))) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxYear"))))
      }
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2017-19")))) shouldBe
          List(RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYear"))))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2015-16")))) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossValidation error" when {
      "an invalid loss type is submitted" in {
        validator.validate(
          requestData
            .CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid")))) shouldBe
          List(TypeOfLossFormatError)
      }

      "there is also a self employment id" in {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", businessId = validBusinessId)))) shouldBe
          List(TypeOfLossFormatError)
      }
    }

    "return RuleBusinessId error" when {
      Seq[TypeOfLoss](
        TypeOfLoss.`uk-property-fhl`,
        TypeOfLoss.`uk-property-non-fhl`
      ).foreach {
        typeOfLoss => s"passed in $typeOfLoss with a valid businessId" in {
          validator.validate(
            requestData.CreateBFLossRawData(
              validNino,
              AnyContentAsJson(Json.parse(
                s"""
                  |{
                  |  "typeOfLoss" : "$typeOfLoss",
                  |  "businessId" : "$validBusinessId",
                  |  "taxYear" : "$validTaxYear",
                  |  "lossAmount" : 1000
                  |}
                  |""".stripMargin))
            )
          ) shouldBe List(RuleBusinessId)
        }
      }
      Seq[TypeOfLoss](
        TypeOfLoss.`self-employment`,
        TypeOfLoss.`self-employment-class4`,
        TypeOfLoss.`foreign-property-fhl-eea`,
        TypeOfLoss.`foreign-property`
      ).foreach {
        typeOfLoss => s"passed in $typeOfLoss with no businessId" in {
          validator.validate(
            requestData.CreateBFLossRawData(
              validNino,
              AnyContentAsJson(Json.parse(
                s"""
                  |{
                  |  "typeOfLoss" : "$typeOfLoss",
                  |  "taxYear" : "$validTaxYear",
                  |  "lossAmount" : 1000
                  |}
                  |""".stripMargin))
            )
          ) shouldBe List(RuleBusinessId)
        }
      }
    }

    "return SelfEmploymentIdValidation error" when {
      "an invalid id is submitted" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(businessId = "invalid")))) shouldBe
          List(BusinessIdFormatError)
      }
    }

    "return AmountValidation error" when {
      "an invalid amount is submitted" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(lossAmount = BigDecimal(100000000000.00))))) shouldBe
          List(RuleInvalidLossAmount)
      }
    }

    "return BusinessIdFormatError error" when {
      "an invalid business id is provided" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino,
            AnyContentAsJson(createRequestBodyJson(businessId = "invalid")))) shouldBe
          List(BusinessIdFormatError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment-class4", businessId = "wrong", taxYear = "2010-11")))) shouldBe
          List(RuleTaxYearNotSupportedError, BusinessIdFormatError)
      }
    }
  }
}
