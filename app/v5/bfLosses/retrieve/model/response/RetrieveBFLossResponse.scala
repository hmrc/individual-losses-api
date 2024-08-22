/*
 * Copyright 2024 HM Revenue & Customs
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

package v5.bfLosses.retrieve.model.response

import play.api.libs.json.{JsValue, OWrites, Reads}
import shared.models.domain.TaxYear
import utils.JsonWritesUtil
import v5.bfLosses.retrieve.RetrieveBFLossResponseSchema
import v5.bfLosses.retrieve.def1.model.response.Def1_RetrieveBFLossResponse
import v5.bfLosses.retrieve.def2.model.response.Def2_RetrieveBFLossResponse

trait RetrieveBFLossResponse

object RetrieveBFLossResponse extends JsonWritesUtil {

  implicit val writes: OWrites[RetrieveBFLossResponse] = writesFrom {
    case a: Def1_RetrieveBFLossResponse => implicitly[OWrites[Def1_RetrieveBFLossResponse]].writes(a)
    case a: Def2_RetrieveBFLossResponse => implicitly[OWrites[Def2_RetrieveBFLossResponse]].writes(a)
  }

  implicit val reads: Reads[RetrieveBFLossResponse] = Reads { json: JsValue =>
    val taxYear = TaxYear.fromDownstream((json \ "taxYear").as[String])

    val schema = RetrieveBFLossResponseSchema.schemaFor(taxYear)

    schema.connectorReads.reads(json)
  }

}
