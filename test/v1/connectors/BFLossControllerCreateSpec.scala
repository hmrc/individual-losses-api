
package v1.connectors

import uk.gov.hmrc.http.HeaderCarrier
import v1.controllers.BFLossController
import org.scalatest.OneInstancePerTest
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BFLossControllerCreateSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockAuditService
  with OneInstancePerTest {

  trait Test {

    val hc = HeaderCarrier()

    val target = new BFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      auditService = mockAuditService
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "create" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test() {


      }
    }
  }
}
