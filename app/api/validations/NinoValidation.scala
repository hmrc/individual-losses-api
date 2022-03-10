package api.validations

import api.models.domain.Nino
import api.models.errors.{MtdError, NinoFormatError}

object NinoValidation {

  private val ninoRegex =
    "^([ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|" +
      "[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}[A-D ]?$"

  def validate(nino: String): List[MtdError] = {
    if (Nino.isValid(nino) && nino.matches(ninoRegex)) NoValidationErrors else List(NinoFormatError)
  }
}
