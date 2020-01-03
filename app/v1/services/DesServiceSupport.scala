/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.services

import play.api.Logger
import v1.connectors.DesOutcome
import v1.models.errors._
import v1.models.outcomes.DesResponse

trait DesServiceSupport {

  /**
    * Service name for logging
    */
  val serviceName: String

  protected val logger: Logger = Logger(this.getClass)

  protected type VendorOutcome[T] = Either[ErrorWrapper, DesResponse[T]]

  /**
    * Gets a function to map DES response outcomes from DES to vendor outcomes.
    *
    * MtdError codes are mapped using the supplied error mapping; success responses are
    * mapped to vendor outcomes using the supplied function.
    *
    * If the DES response body domain object should be used directly in the vendor outcome,
    * use mapToVendorDirect
    *
    * @param endpointName endpoint name for logging
    * @param errorMap     mapping from DES error codes to vendor (MTD) errors
    * @param success      mapping for a success DES response
    * @tparam D the DES response domain object type
    * @tparam V the vendor response domain object type
    * @return the function to map outcomes
    */
  final def mapToVendor[D, V](endpointName: String, errorMap: PartialFunction[String, MtdError])(success: DesResponse[D] => VendorOutcome[V])(
      desOutcome: DesOutcome[D]): VendorOutcome[V] = {

    lazy val defaultErrorMapping: String => MtdError = { code =>
      logger.info(s"[$serviceName] [$endpointName] - No mapping found for error code $code")
      DownstreamError
    }

    desOutcome match {
      case Right(desResponse) => success(desResponse)

      case Left(DesResponse(correlationId, MultipleErrors(errors))) =>
        val mtdErrors = errors.map(error => errorMap.applyOrElse(error.code, defaultErrorMapping))

        if (mtdErrors.contains(DownstreamError)) {
          logger.info(
            s"[$serviceName] [$endpointName] [CorrelationId - $correlationId]" +
              s" - downstream returned ${errors.map(_.code).mkString(",")}. Revert to ISE")
          Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
        } else {
          Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(mtdErrors)))
        }

      case Left(DesResponse(correlationId, SingleError(error))) =>
        Left(ErrorWrapper(Some(correlationId), errorMap.applyOrElse(error.code, defaultErrorMapping), None))

      case Left(DesResponse(correlationId, OutboundError(error))) =>
        Left(ErrorWrapper(Some(correlationId), error, None))
    }
  }

  /**
    * Gets a function to map DES response outcomes from DES to vendor outcomes.
    *
    * MtdError codes are mapped using the supplied error mapping.
    *
    * Success responses are
    * mapped directly to vendor outcomes unchanged.
    *
    * @param endpointName endpoint name for logging
    * @param errorMap     mapping from DES error codes to vendor (MTD) errors
    * @tparam D the DES response domain object type
    * @return the function to map outcomes
    */
  final def mapToVendorDirect[D](endpointName: String, errorMap: PartialFunction[String, MtdError])(
      desOutcome: DesOutcome[D]): VendorOutcome[D] =
    mapToVendor[D, D](endpointName, errorMap) { desResponse =>
      Right(DesResponse(desResponse.correlationId, desResponse.responseData))
    }(desOutcome)

}
