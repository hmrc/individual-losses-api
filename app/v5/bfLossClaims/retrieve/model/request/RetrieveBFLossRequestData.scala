
package v5.bfLossClaims.retrieve.model.request

import api.models.domain.Nino
import v5.bfLossClaims.retrieve.model.LossId

trait RetrieveBFLossRequestData {
  def nino: Nino
  def lossId: LossId
}
