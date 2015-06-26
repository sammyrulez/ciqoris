package ciqoris


import akka.actor.{Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._



class DummyCommandValidator extends CommandValidator[Any] {
  override def validate(command: Command[Any]): Either[String, String] = Right("ok")

  override def acceptCommand(command: Command[_]): Boolean = {true}
}


case class CommandHandler[T](val command: Command[T])

class CommandValidatingActor(val validators:List[CommandValidator[_]] )extends Actor {

  def findValidator(command: Command[_]): CommandValidator[_] = {
    val filtered: List[CommandValidator[_]] = validators.filter(validator => validator.acceptCommand(command))
    filtered.size match {
      case 1 => filtered.head
      case 0 => new DummyCommandValidator()
      case _ => throw new RuntimeException("More than one validator for command " + command.name + " : " + filtered.toString)
    }
  }

  override def receive: Receive = {
    case CommandHandler(command) => {
      println("CommandValidatingActor: " + command.name)

      val validator: CommandValidator[_] = findValidator(command)

      sender ! validator.asInstanceOf[CommandValidator[Any]].validate(command)
    }
    case _ =>{
      println("CommandValidatingActor wrong message " )
    }
  }
}

class ActionExecutingActor(val executor:CommandExecutor[_])extends Actor{



  override def receive: Actor.Receive = {

    case CommandHandler(command) => {
      println("ActionExecutingActor: " + command.name + " with executor " + executor.toString)
      executor.asInstanceOf[CommandExecutor[Any]].execute(command)

    }

    case _ =>{
      println("ActionExecutingActor  wrong message " )
    }
  }
}

class CommandExecutingActor(val executors:List[CommandExecutor[_]])extends Actor{



  override def receive: Actor.Receive = {

    case CommandHandler(command) => {
      println("CommandExecutingActor: " + command.name )
      val filtered = executors.filter(exec => exec.acceptCommand(command))
      println("found : " + filtered.size + " suitable executors")
      val commandHandler = new CommandHandler(command)
      filtered.foreach(exec => {
        val executingProps: Props = Props(classOf[ActionExecutingActor],exec)
        val actionActor =context.actorOf(executingProps)
        actionActor ! commandHandler
      })


    }

    case _ =>{
      println("CommandExecutingActor  wrong message " )
    }
  }
}

class CommandPersistActor(val executors:List[CommandExecutor[_]]) extends Actor {



  override def receive: Actor.Receive = {
    case CommandHandler(command) => {
      println("Persisting: " + command.name)

      val executingProps: Props = Props(classOf[CommandExecutingActor],executors)
      val execActor =context.actorOf(executingProps)
      execActor ! new CommandHandler(command)


    }

    case _ =>{
      println("CommandPersistActor  wrong message " )
    }
  }
}


class CommandHandlerActor(val validators:List[CommandValidator[_]],val executors:List[CommandExecutor[_]]) extends Actor{



  val validatingProps: Props = Props(classOf[CommandValidatingActor],validators)

  val eventPersistProps:Props = Props(classOf[CommandPersistActor],executors)

  override def receive: Actor.Receive = {

    case CommandHandler(command) => {
      println("Evaluating : "  + command.name)
      val validatingActor =context.actorOf(validatingProps)
      implicit val timeout = Timeout(5 seconds)
      val future = validatingActor ? new CommandHandler(command)
      val result:Either[String,String] = Await.result(future, timeout.duration).asInstanceOf[Either[String,String]]
      result match {
        case Right(out) => {
          val persistActor = context.actorOf(eventPersistProps)
          persistActor ! new CommandHandler(command)

        }
        case Left(_) => println("Error")
      }
      sender ! result

    }

  }
}
