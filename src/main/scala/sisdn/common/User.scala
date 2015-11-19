package sisdn.common

case class User(
  subject: String,
  org: String,
  departments: Option[Set[Int]],
  faculties: Option[Set[Int]],
  claims: Option[Set[String]]
)
