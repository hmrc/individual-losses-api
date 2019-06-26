/*
 * Copyright 2019 HM Revenue & Customs
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

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"
  private val validTypeOfLoss = "self-employment"
  private val validSelfEmploymentId = "XAIS01234567890"

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
      case None => s"""{
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
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2017-19")))) shouldBe
          List(RuleTaxYearRangeExceededError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYear = "2016-17")))) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return TypeOfLossValidation error" when {
      "an invalid loss type is submitted" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", selfEmploymentId = None)))) shouldBe
          List(RuleTypeOfLossUnsupported)
      }
    }

    "return SelfEmploymentIdValidation error" when {
      "an invalid id is submitted" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(selfEmploymentId = Some("invalid"))))) shouldBe
          List(RuleInvalidSelfEmploymentId)
      }
    }

    "return AmountValidation error" when {
      "an invalid amount is submitted" in {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(lossAmount = BigDecimal(100000000000.00))))) shouldBe
          List(RuleInvalidLossAmount)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid")))) shouldBe
          List(RuleTypeOfLossUnsupported, RulePropertySelfEmploymentId)
      }
    }
  }
}