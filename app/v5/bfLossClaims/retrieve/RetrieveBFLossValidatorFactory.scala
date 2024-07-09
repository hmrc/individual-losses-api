
package v5.bfLossClaims.retrieve

import api.controllers.validators.Validator
import v5.bfLossClaims.retrieve.RetrieveBFLossSchema.Def1
import v5.bfLossClaims.retrieve.def1.Def1_RetrieveBFLossValidator
import v5.bfLossClaims.retrieve.model.request.RetrieveBFLossRequestData

class RetrieveBFLossValidatorFactory {
  def validator(nino: String, body: String): Validator[RetrieveBFLossRequestData] = {
    val schema = RetrieveBFLossSchema.schema
    schema match {
      case Def1 => new Def1_RetrieveBFLossValidator(nino, body)
    }
  }
}
