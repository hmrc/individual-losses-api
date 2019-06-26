
package v1.models.outcomes

case class DesResponse[+T](correlationId: String, responseData: T)
