package sisdn.common

case class User(
  username: String,
  org: String,
  departments: Option[Set[String]],
  faculties: Option[Set[String]],
  claims: Option[Set[String]]
)
