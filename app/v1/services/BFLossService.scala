
package v1.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v1.connectors.DesConnector
import v1.models.requestData.CreateBFLossRequest

import scala.concurrent.{ExecutionContext, Future}

class BFLossService @Inject()(connector: DesConnector) {

  def create(createBFLossRequest: CreateBFLossRequest)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RetrieveCharitableGivingOutcome] = ???

}
