
package v5.bfLossClaims.create.def1.model.request

import api.models.domain.Nino
import v5.bfLossClaims.create.CreateBFLossSchema
import v5.bfLossClaims.create.model.request.CreateBFLossRequestData

case class Def1_CreateBFLossRequestData (nino: Nino, broughtForwardLoss: Def1_CreateBFLossRequestBody) extends CreateBFLossRequestData {
  val schema: CreateBFLossSchema = CreateBFLossSchema.Def1
}
