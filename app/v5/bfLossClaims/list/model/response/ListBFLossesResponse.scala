
package v5.bfLossClaims.list.model.response

import play.api.libs.json.{Json, OWrites}
import utils.JsonWritesUtil

trait ListBFLossesResponse[I <: ListBFLossesItem]

object ListBFLossesResponse extends JsonWritesUtil {
  implicit val writes: OWrites[ListBFLossesResponse[ListBFLossesItem]] = Json.writes[ListBFLossesResponse[ListBFLossesItem]]
}

