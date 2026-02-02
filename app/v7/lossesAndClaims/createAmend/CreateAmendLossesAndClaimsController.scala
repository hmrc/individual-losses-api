package v7.lossesAndClaims.createAmend

import config.LossesFeatureSwitches
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.config.SharedAppConfig
import shared.controllers.*
import shared.controllers.validators.Validator
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v7.lossesAndClaims.createAmend.request.CreateAmendLossesAndClaimsRequestData

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CreateAmendLossesAndClaimsController @Inject() (val authService: EnrolmentsAuthService,
                                                      val lookupService: MtdIdLookupService,
                                                      service: CreateAmendLossesAndClaimsService,
                                                      validatorFactory: CreateAmendLossesAndClaimsValidationFactory,
                                                      auditService: AuditService,
                                                      cc: ControllerComponents,
                                                      idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: SharedAppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "create-and-amend-losses-and-claims"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateAmendLossesAndClaimsController", endpointName = " Create and Amend Losses And Claims")

  def createAndAmend(nino: String, businessId: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator: Validator[CreateAmendLossesAndClaimsRequestData] = validatorFactory.validator(
        nino = nino,
        businessId = businessId,
        taxYear = taxYear,
        body = request.body
      )
      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.createAmendLossesAndClaims)
          .withNoContentResult()
          .withAuditing(AuditHandler(
            auditService,
            auditType = "CreateAmendLossesAndClaims",
            transactionName = "create-and-amend-losses-and-claims",
            apiVersion = Version(request),
            params = Map("nino" -> nino, "businessId" -> businessId, "taxYear" -> taxYear),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
