package sisdn.common

case class User(
  username: String,
  org: String,
  departments: Option[Set[Int]],
  faculties: Option[Set[Int]],
  claims: Option[Set[String]]
)
