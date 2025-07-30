/*
 * Copyright 2023 HM Revenue & Customs
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

package v5.lossClaims.create.model.request

import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Json}
import shared.config.MockSharedAppConfig
import shared.models.domain.TaxYear
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v5.lossClaims.common.models.*
import v5.lossClaims.create.def1.model.request.Def1_CreateLossClaimRequestBody

class CreateLossClaimRequestBodySpec extends UnitSpec with JsonErrorValidators with MockSharedAppConfig {

  val lossClaimSelfEmployment: Def1_CreateLossClaimRequestBody = Def1_CreateLossClaimRequestBody(
    taxYearClaimedFor = "2019-20",
    typeOfLoss = TypeOfLoss.`self-employment`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    businessId = "X2IS12356589871"
  )

  val lossClaimSelfEmploymentJson: JsValue = Json.parse("""
      |{
      |    "typeOfLoss": "self-employment",
      |    "businessId": "X2IS12356589871",
      |    "typeOfClaim": "carry-forward",
      |    "taxYearClaimedFor": "2019-20"
      |}
    """.stripMargin)

  val lossClaimUkPropertyNonFhl: Def1_CreateLossClaimRequestBody = Def1_CreateLossClaimRequestBody(
    taxYearClaimedFor = "2019-20",
    typeOfLoss = TypeOfLoss.`uk-property`,
    typeOfClaim = TypeOfClaim.`carry-forward-to-carry-sideways`,
    businessId = "X2IS12356589871"
  )

  val lossClaimUkPropertyNonFhlJson: JsValue = Json.parse("""
      |{
      |    "businessId": "X2IS12356589871",
      |    "typeOfLoss": "uk-property",
      |    "typeOfClaim": "carry-forward-to-carry-sideways",
      |    "taxYearClaimedFor": "2019-20"
      |}
    """.stripMargin)

  val lossClaimForeignProperty: Def1_CreateLossClaimRequestBody = Def1_CreateLossClaimRequestBody(
    taxYearClaimedFor = "2019-20",
    typeOfLoss = TypeOfLoss.`foreign-property`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    businessId = "X2IS12356589871"
  )

  val lossClaimForeignPropertyJson: JsValue = Json.parse("""
      |{
      |    "typeOfLoss": "foreign-property",
      |    "businessId": "X2IS12356589871",
      |    "typeOfClaim": "carry-forward",
      |    "taxYearClaimedFor": "2019-20"
      |}
    """.stripMargin)

  def expectedDownstreamJson(isHipEnabled: Boolean,
                             mtdTaxYearClaimedFor: String,
                             incomeSourceId: String,
                             reliefClaimed: String,
                             incomeSourceType: Option[String] = None): JsObject = {
    val baseJson: JsObject = Json.obj(
      "reliefClaimed"  -> reliefClaimed,
      "incomeSourceId" -> incomeSourceId
    )

    val typeOfLossJson: JsObject = incomeSourceType.map(t => Json.obj("incomeSourceType" -> t)).getOrElse(Json.obj())

    val taxYearClaimedForJson: JsObject = if (isHipEnabled) {
      Json.obj("taxYearClaimedFor" -> TaxYear.fromMtd(mtdTaxYearClaimedFor).year)
    } else {
      Json.obj("taxYear" -> TaxYear.fromMtd(mtdTaxYearClaimedFor).asDownstream)
    }

    baseJson ++ typeOfLossJson ++ taxYearClaimedForJson
  }

  "reads" when {
    "given a valid LossClaim Json" should {
      "return a valid model" when {
        Map[Def1_CreateLossClaimRequestBody, JsValue](
          lossClaimSelfEmployment   -> lossClaimSelfEmploymentJson,
          lossClaimUkPropertyNonFhl -> lossClaimUkPropertyNonFhlJson,
          lossClaimForeignProperty  -> lossClaimForeignPropertyJson
        )
          .foreach { case (model, json) =>
            s"typeOfLoss = ${model.typeOfLoss}" in {
              json.as[Def1_CreateLossClaimRequestBody] shouldBe model
            }
          }
      }

      testPropertyType[Def1_CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)(
        path = "/typeOfLoss",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testPropertyType[Def1_CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)(
        path = "/taxYearClaimedFor",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testPropertyType[Def1_CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)(
        path = "/typeOfClaim",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "given a valid Loss Claim Self Employment model" should {
      List(true, false).foreach { isHipEnabled =>
        s"return a valid Loss Claim Self Employment JSON when feature switch is set to $isHipEnabled" in {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1505.enabled" -> isHipEnabled)

          Json.toJson(lossClaimSelfEmployment) shouldBe expectedDownstreamJson(
            isHipEnabled = isHipEnabled,
            mtdTaxYearClaimedFor = "2019-20",
            incomeSourceId = "X2IS12356589871",
            reliefClaimed = "CF")
        }
      }
    }
    "given a valid Loss Claim UK Property model" should {
      List(true, false).foreach { isHipEnabled =>
        s"return a valid Loss Claim UK Property JSON when feature switch is set to $isHipEnabled" in {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1505.enabled" -> isHipEnabled)

          Json.toJson(lossClaimUkPropertyNonFhl) shouldBe expectedDownstreamJson(
            isHipEnabled = isHipEnabled,
            mtdTaxYearClaimedFor = "2019-20",
            incomeSourceId = "X2IS12356589871",
            reliefClaimed = "CFCSGI",
            incomeSourceType = Some("02")
          )
        }
      }
    }
    "given a valid Loss Claim Foreign Property model" should {
      List(true, false).foreach { isHipEnabled =>
        s"return a valid Loss Claim Foreign Property JSON when feature switch is set to $isHipEnabled" in {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1505.enabled" -> isHipEnabled)

          Json.toJson(lossClaimForeignProperty) shouldBe expectedDownstreamJson(
            isHipEnabled = isHipEnabled,
            mtdTaxYearClaimedFor = "2019-20",
            incomeSourceId = "X2IS12356589871",
            reliefClaimed = "CF",
            incomeSourceType = Some("15")
          )
        }
      }
    }
  }

}
