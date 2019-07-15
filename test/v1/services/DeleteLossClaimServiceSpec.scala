
package v1.services

import uk.gov.hmrc.domain.Nino
import v1.mocks.connectors.MockDesConnector

class DeleteLossClaimServiceSpec {


  val nino = Nino("AA123456A")
  val claimId = "AAZZ1234567890a"

  trait Test extends MockDesConnector {
    lazy val service = new DeleteLossClaimService(connector)
  }
}
