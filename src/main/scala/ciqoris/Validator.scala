package ciqoris


class BypassCommandValidator extends CommandValidator[Any] {
  //override def validate(command: Command[Any]): Either[String, String] = Right("ok")


  override def validate(command: Command[Any]): Either[List[Error], String] = Right("Ok " + command.id)

  override def acceptCommand(command: Command[_]): Boolean = {
    true
  }
}

trait CommandValidator[T] extends CommandReciver {

  def validate(command: Command[T]): Either[List[Error], String]

}

case class Error(val code: String, val message: String, val field: Option[String])


class Validator {

}
