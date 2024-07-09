
package v5.bfLossClaims.list.model.response

import v5.bfLossClaims.list.model.TypeOfLoss

trait ListBFLossesItem {
  def lossId: String
  def businessId: String
  def typeOfLoss: TypeOfLoss
  def lossAmount: BigDecimal
  def taxYearBroughtForwardFrom: String
  def lastModified: String
}
