package ciqoris

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner._

import scala.util


class DummyCommandValidator extends CommandValidator[String] {


  override def validate(command: Command[String]): Either[List[Error], String] = {
    if (command.payload.equals("wrong"))
      Left(List(new Error("0", "Wrong command data", None)))
    else
      Right(command.payload)
  }

  override def acceptCommand(command: Command[_]): Boolean = {true}
}

class DummyCommandExecutor(var state:String) extends CommandExecutor[String] {
  override def execute(command: Command[String]): Unit = {
    println("************************** Executing***************************")
    println(command.name + "(" + command.id +") " + command.payload + "\n")
    state = command.payload
  }

  override def acceptCommand(command: Command[_]): Boolean = {true}
}

class DummyPersister() extends EventPersister[String] {
  override def persistEvent(event:Event[String]) = {
    println("************************** PERSISTING***************************")
    println(event.command.name + "(" + event.command.id +") " + event.command.payload + "\n")

  }
}

@RunWith(classOf[JUnitRunner])
class CommandTest extends Specification {

  val command: Command[String] = new Command[String]("dummy","8373AB32F","personX")
  val commandWrong: Command[String] = new Command[String]("dummy", "8373AB32F", "wrong")
  val validator = new DummyCommandValidator()

  "A command " should {
    "be validated" in {

      val result = validator.validate(command)
      result must beRight("personX")
    }
    "or not" in {
      val result = validator.validate(commandWrong)
      result must beLeft(List(new Error("0", "Wrong command data", None)))
    }
    "be executed" in {
      val executor = new DummyCommandExecutor("empty")
      executor.execute(command)
      executor.state mustEqual  "personX"
    }
    "be stored" in {

      val persister = new DummyPersister
      persister.persist("Test",command)

    }

  }
}
