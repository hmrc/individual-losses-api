package api.services

import play.api.http.{ HeaderNames, MimeTypes, Status }
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait ServiceSpec extends UnitSpec with Status with MimeTypes with HeaderNames {

  val correlationId: String         = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

}
