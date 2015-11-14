package sisdn.common

class DuplicateEntityException private(ex: RuntimeException) extends RuntimeException(ex) {
  def this(message:String) = this(new RuntimeException(message))
  def this(message:String, throwable: Throwable) = this(new RuntimeException(message, throwable))
}

object DuplicateEntityException {
  def apply(message:String) = new DuplicateEntityException(message)
  def apply(message:String, throwable: Throwable) = new DuplicateEntityException(message, throwable)
}
