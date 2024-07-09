
package v5.bfLossClaims.retrieve.model.response

import play.api.libs.json.OWrites
import utils.JsonWritesUtil
import v5.bfLossClaims.retrieve.def1.model.response.Def1_RetrieveBFLossResponse

trait RetrieveBFLossResponse

object RetrieveBFLossResponse extends JsonWritesUtil {
  implicit val writes: OWrites[RetrieveBFLossResponse] = writesFrom { case a: Def1_RetrieveBFLossResponse =>
    implicitly[OWrites[Def1_RetrieveBFLossResponse]].writes(a)
  }
}
