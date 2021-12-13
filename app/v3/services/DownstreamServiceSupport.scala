/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.services

import play.api.Logger
import v3.connectors.IfsOutcome
import v3.models.errors._
import v3.models.outcomes.ResponseWrapper

trait DownstreamServiceSupport {

  /**
    * Service name for logging
    */
  val serviceName: String

  protected val logger: Logger = Logger(this.getClass)

  protected type VendorOutcome[T] = Either[ErrorWrapper, ResponseWrapper[T]]

  /**
    * Gets a function to map downstream response outcomes from downstream to vendor outcomes.
    *
    * MtdError codes are mapped using the supplied error mapping; success responses are
    * mapped to vendor outcomes using the supplied function.
    *
    * If the downstream response body domain object should be used directly in the vendor outcome,
    * use mapToVendorDirect
    *
    * @param endpointName endpoint name for logging
    * @param errorMap     mapping from downstream error codes to vendor (MTD) errors
    * @param success      mapping for a success downstream response
    * @tparam D the downstream response domain object type
    * @tparam V the vendor response domain object type
    * @return the function to map outcomes
    */
  final def mapToVendor[D, V](endpointName: String, errorMap: PartialFunction[String, MtdError])(success: ResponseWrapper[D] => VendorOutcome[V])(
    downstreamOutcome: IfsOutcome[D]): VendorOutcome[V] = {

    lazy val defaultErrorMapping: String => MtdError = { code =>
      logger.warn(s"[$serviceName] [$endpointName] - No mapping found for error code $code")
      DownstreamError
    }

    downstreamOutcome match {
      case Right(downstreamResponse) => success(downstreamResponse)

      case Left(ResponseWrapper(correlationId, MultipleErrors(errors))) =>
        val mtdErrors = errors.map(error => errorMap.applyOrElse(error.code, defaultErrorMapping))

        if (mtdErrors.contains(DownstreamError)) {
          logger.warn(
            s"[$serviceName] [$endpointName] [CorrelationId - $correlationId]" +
              s" - downstream returned ${errors.map(_.code).mkString(",")}. Revert to ISE")
          Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
        } else {
          Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(mtdErrors)))
        }

      case Left(ResponseWrapper(correlationId, SingleError(error))) =>
        Left(ErrorWrapper(Some(correlationId), errorMap.applyOrElse(error.code, defaultErrorMapping), None))

      case Left(ResponseWrapper(correlationId, OutboundError(error))) =>
        Left(ErrorWrapper(Some(correlationId), error, None))
    }
  }

  /**
    * Gets a function to map downstream response outcomes from downstream to vendor outcomes.
    *
    * MtdError codes are mapped using the supplied error mapping.
    *
    * Success responses are
    * mapped directly to vendor outcomes unchanged.
    *
    * @param endpointName endpoint name for logging
    * @param errorMap     mapping from downstream error codes to vendor (MTD) errors
    * @tparam D the downstream response domain object type
    * @return the function to map outcomes
    */
  final def mapToVendorDirect[D](endpointName: String, errorMap: PartialFunction[String, MtdError])(
    downstreamOutcome: IfsOutcome[D]): VendorOutcome[D] =
    mapToVendor[D, D](endpointName, errorMap) { downstreamResponse =>
      Right(ResponseWrapper(downstreamResponse.correlationId, downstreamResponse.responseData))
    }(downstreamOutcome)

}
