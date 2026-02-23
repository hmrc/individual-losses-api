/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.retrieve.fixtures

import play.api.libs.json.{JsValue, Json}
import shared.models.domain.Timestamp
import v7.lossesAndClaims.commons.PreferenceOrderEnum.`carry-back`
import v7.lossesAndClaims.retrieve.model.response.*

object RetrieveLossesAndClaimsFixtures {

  val responseBodyModel: RetrieveLossesAndClaimsResponse = RetrieveLossesAndClaimsResponse(
    submittedOn = Timestamp("2026-08-24T14:15:22.544Z"),
    claims = Some(
      Claims(
        carryBack = Some(
          CarryBack(
            previousYearGeneralIncome = Some(5000.99),
            earlyYearLosses = Some(5000.99),
            terminalLosses = Some(5000.99)
          )
        ),
        carrySideways = Some(
          CarrySideways(
            currentYearGeneralIncome = Some(5000.99)
          )
        ),
        preferenceOrder = Some(
          PreferenceOrder(
            applyFirst = Some(`carry-back`)
          )
        ),
        carryForward = Some(
          CarryForward(
            currentYearLosses = Some(5000.99),
            previousYearsLosses = Some(5000.99)
          )
        )
      )
    ),
    losses = Some(
      Losses(
        broughtForwardLosses = Some(5000.99)
      )
    )
  )

  val mtdResponseBodyJson: JsValue = Json.parse(
    """
      |{
      |  "submittedOn": "2026-08-24T14:15:22.544Z",
      |  "claims": {
      |    "carryBack": {
      |      "previousYearGeneralIncome": 5000.99,
      |      "earlyYearLosses": 5000.99,
      |      "terminalLosses": 5000.99
      |    },
      |    "carrySideways": {
      |      "currentYearGeneralIncome": 5000.99
      |    },
      |    "preferenceOrder": {
      |      "applyFirst": "carry-back"
      |    },
      |    "carryForward": {
      |      "currentYearLosses": 5000.99,
      |      "previousYearsLosses": 5000.99
      |    }
      |  },
      |  "losses": {
      |    "broughtForwardLosses": 5000.99
      |  }
      |}
    """.stripMargin
  )

  val downstreamResponseBodyJson: JsValue = Json.parse(
    """
      |{
      |  "submittedOn": "2026-08-24T14:15:22.544Z",
      |  "claims": {
      |    "carryBack": {
      |      "previousYearGeneralIncomeSection64": 5000.99,
      |      "earlyYearLossesSection72": 5000.99,
      |      "terminalLossesSection89": 5000.99
      |    },
      |    "carrySideways": {
      |      "currentYearGeneralIncomeSection64": 5000.99
      |    },
      |    "preferenceOrderSection64": {
      |      "applyFirst": "carry-back"
      |    },
      |    "carryForward": {
      |      "currentYearLossesSection83": 5000.99,
      |      "previousYearsLossesSection83": 5000.99
      |    }
      |  },
      |  "losses": {
      |    "broughtForwardLosses": 5000.99
      |  }
      |}
    """.stripMargin
  )

}
