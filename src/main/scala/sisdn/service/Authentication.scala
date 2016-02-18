package sisdn.service

import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Date

import authentikat.jwt.JsonWebToken
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import sisdn.common.{User, UserJsonProtocol}
import sisdn.utils.Base64
import spray.json.JsonParser

trait Authentication extends UserJsonProtocol {
  val secret: String
  val appEnv: String

  def userExtractor(cred: Option[String]): Option[User] = cred match {
    case Some(token) => {
      try {
        val keyBytes = Base64.decode(secret)
        val spec = new X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactory.generatePublic(spec).asInstanceOf[RSAPublicKey]

        val jwt = SignedJWT.parse(token)

        if (jwt.verify(new RSASSAVerifier(key))) {
          jwt.getParsedString match {
            case JsonWebToken(_, claimsSet, _) =>
              if(appEnv == "dist" && new Date().before(jwt.getJWTClaimsSet.getExpirationTime))
                Some(JsonParser(claimsSet.asJsonString).convertTo[User])
              else if(appEnv == "dist" && new Date().after(jwt.getJWTClaimsSet.getExpirationTime))
                None
              else
                Some(JsonParser(claimsSet.asJsonString).convertTo[User])
            case _ => None
          }
        }
        else None
      }
      catch {
        case e: Throwable => None
      }
    }
    case None => None
  }
}
