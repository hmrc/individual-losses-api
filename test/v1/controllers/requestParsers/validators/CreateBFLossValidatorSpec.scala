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

package v1.controllers.requestParsers.validators

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors._
import v1.models.requestData
import v1.models.requestData.CreateBFLossRawData

class CreateBFLossValidatorSpec extends UnitSpec {

  private val validNino             = "AA123456A"
  private val validTaxYear          = "2018-19"
  private val validTypeOfLoss       = "self-employment"
  private val validSelfEmploymentId = "XAIS01234567890"

  val emptyBody: JsValue = Json.parse(
    s"""{
       |
       |
       |}""".stripMargin
  )

  def createRequestBodyJson(typeOfLoss: String = validTypeOfLoss,
                            selfEmploymentId: Option[String] = Some(validSelfEmploymentId),
                            taxYear: String = validTaxYear,
                            lossAmount: BigDecimal = 1000): JsValue = Json.parse(
    selfEmploymentId match {
      case Some(id) => s"""{
                          |  "typeOfLoss" : "$typeOfLoss",
                          |  "selfEmploymentId" : "$id",
                          |  "taxYear" : "$taxYear",
                          |  "lossAmount" : $lossAmount
                          |}""".stripMargin
      case None     => s"""{
                      |  "typeOfLoss" : "$typeOfLoss",
                      |  "taxYear" : "$taxYear",
                      |  "lossAmount" : $lossAmount
                      |}""".stripMargin
    }
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
          List(TaxYearFormatError)
      }

      "a non-numeric tax year is supplied" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "XXXX-YY")))) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2017-19")))) shouldBe
          List(RuleTaxYearRangeInvalid)
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
            .CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", selfEmploymentId = None)))) shouldBe
          List(TypeOfLossFormatError)
      }

      "there is also a self employment id" in {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", selfEmploymentId = Some(validSelfEmploymentId))))) shouldBe
          List(TypeOfLossFormatError)
      }
    }

    "return SelfEmploymentIdValidation error" when {
      "an invalid id is submitted" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(selfEmploymentId = Some("invalid"))))) shouldBe
          List(SelfEmploymentIdFormatError)
      }
    }

    "return AmountValidation error" when {
      "an invalid amount is submitted" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(lossAmount = BigDecimal(100000000000.00))))) shouldBe
          List(RuleInvalidLossAmount)
      }
    }

    "return RuleSelfEmploymentId error" when {
      "a self-employment is not provided for a self-employment loss type" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino,
                                          AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment", selfEmploymentId = None)))) shouldBe
          List(RuleSelfEmploymentId)
      }

      "a self-employment is not provided for a self-employment-class4 loss type" in {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment-class4", selfEmploymentId = None)))) shouldBe
          List(RuleSelfEmploymentId)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment-class4", selfEmploymentId = None, taxYear = "2010-11")))) shouldBe
          List(RuleTaxYearNotSupportedError, RuleSelfEmploymentId)
      }
    }
  }
}
