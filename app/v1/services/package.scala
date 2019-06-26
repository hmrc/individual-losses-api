
package v1

import v1.models.des.CreateBFLossResponse
import v1.models.errors.ErrorWrapper
import v1.models.outcomes.DesResponse

package object services {
  type RetrieveCharitableGivingOutcome = Either[ErrorWrapper, DesResponse[CreateBFLossResponse]]


}
