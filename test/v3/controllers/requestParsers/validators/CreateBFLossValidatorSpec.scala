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

package v3.controllers.requestParsers.validators

import com.typesafe.config.ConfigFactory
import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import utils.CurrentDateTime
import v3.mocks.MockCurrentDateTime
import v3.models.errors._
import v3.models.requestData
import v3.models.requestData.CreateBFLossRawData

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
                            taxYearBroughtForwardFrom: String = validTaxYear,
                            lossAmount: BigDecimal = 1000): JsValue = Json.parse(
    s"""{
       |  "typeOfLoss" : "$typeOfLoss",
       |  "businessId" : "$businessId",
       |  "taxYearBroughtForwardFrom" : "$taxYearBroughtForwardFrom",
       |  "lossAmount" : $lossAmount
       |}""".stripMargin
  )

  class Test(errorFeatureSwitch: Boolean = true) extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator: CreateBFLossValidator = new CreateBFLossValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2021-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockAppConfig.featureSwitch.returns(Some(Configuration(ConfigFactory.parseString(
      s"""
         |taxYearNotEndedRule.enabled = $errorFeatureSwitch
      """.stripMargin))))
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson()))) shouldBe Nil
      }
    }

    "return IncorrectOrEmptyBodySubmitted error" when {
      "an incorrect or empty body is supplied" in new Test {
        validator.validate(CreateBFLossRawData(validNino, AnyContentAsJson(emptyBody))) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(requestData.CreateBFLossRawData("A12344A", AnyContentAsJson(createRequestBodyJson()))) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYearBroughtForwardFrom = "2016")))) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxYearBroughtForwardFrom"))))
      }

      "a non-numeric tax year is supplied" in new Test {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYearBroughtForwardFrom = "XXXX-YY")))) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxYearBroughtForwardFrom"))))
      }
    }

    "return RuleTaxYearRangeExceededError error" when {
      "a tax year is provided with a range greater than a year" in new Test {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYearBroughtForwardFrom = "2017-19")))) shouldBe
          List(RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYearBroughtForwardFrom"))))
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in new Test {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYearBroughtForwardFrom = "2015-16")))) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return RuleTaxYearNotEndedError error" when {
      "an out of range tax year is supplied" in new Test {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(taxYearBroughtForwardFrom = "2022-23")))) shouldBe
          List(RuleTaxYearNotEndedError)
      }
    }

    "return TypeOfLossValidation error" when {
      "an invalid loss type is submitted" in new Test {
        validator.validate(
          requestData
            .CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid")))) shouldBe
          List(TypeOfLossFormatError)
      }

      "there is also a self employment id" in new Test {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "invalid", businessId = validBusinessId)))) shouldBe
          List(TypeOfLossFormatError)
      }
    }

    "return AmountValidation error" when {
      "an invalid amount (Too high) is submitted" in new Test {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(lossAmount = BigDecimal(100000000000.00))))) shouldBe
          List(ValueFormatError.copy(paths = Some(List("/lossAmount"))))
      }
    }

    "return AmountValidation error" when {
      "an invalid amount (Negative) is submitted" in new Test {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(lossAmount = BigDecimal(-100.00))))) shouldBe
          List(ValueFormatError.copy(paths = Some(List("/lossAmount"))))
      }
    }

    "return AmountValidation error" when {
      "an invalid amount (3 decimal places) is submitted" in new Test {
        validator.validate(
          requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(lossAmount = BigDecimal(100.734))))) shouldBe
          List(ValueFormatError.copy(paths = Some(List("/lossAmount"))))
      }
    }

    "return BusinessIdFormatError error" when {
      "an invalid business id is provided" in new Test {
        validator.validate(requestData.CreateBFLossRawData(validNino, AnyContentAsJson(createRequestBodyJson(businessId = "invalid")))) shouldBe
          List(BusinessIdFormatError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in new Test {
        validator.validate(
          requestData.CreateBFLossRawData(
            validNino,
            AnyContentAsJson(createRequestBodyJson(typeOfLoss = "self-employment-class4", businessId = "wrong", taxYearBroughtForwardFrom = "2010-11")))) shouldBe
          List(RuleTaxYearNotSupportedError, BusinessIdFormatError)
      }
    }
  }
}