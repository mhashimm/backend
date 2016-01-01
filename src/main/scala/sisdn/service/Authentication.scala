package sisdn.service

import authentikat.jwt.JsonWebToken
import sisdn.common.{UserJsonProtocol, User}
import spray.json.JsonParser

trait Authentication extends UserJsonProtocol {
  val secret: String

  def userExtractor(cred: Option[String]): Option[User] = cred match {
    case Some(JsonWebToken(_, claimsSet, _)) if JsonWebToken.validate(cred.get, secret) => {
      try {
        Some(JsonParser(claimsSet.asJsonString).convertTo[User])
      }
      catch {
        case _: Throwable => None
      }
    }
    case _ => None
  }
}
