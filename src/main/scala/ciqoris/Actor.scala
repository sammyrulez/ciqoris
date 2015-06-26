package ciqoris


import akka.actor.{Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scaldi.Injector
import scala.concurrent.{Await}
import scala.concurrent.duration._
import scaldi.akka.AkkaInjectable._



class DummyCommandValidator extends CommandValidator[Any] {
  override def validate(command: Command[Any]): Either[String, String] = Right("ok")

  override def acceptCommand(command: Command[_]): Boolean = {true}
}


case class CommandHandler[T](val command: Command[T])

class CommandValidatingActor(val validators:List[CommandValidator[_]] )extends Actor  with Logging{

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
      logInfo("CommandValidatingActor: " + command.name)

      val validator: CommandValidator[_] = findValidator(command)

      sender ! validator.asInstanceOf[CommandValidator[Any]].validate(command)
    }
    case _ =>{
      logError("CommandValidatingActor wrong message " )
    }
  }
}

class ActionExecutingActor(val executor:CommandExecutor[_])extends Actor with Logging{



  override def receive: Actor.Receive = {

    case CommandHandler(command) => {
      logInfo("ActionExecutingActor: " + command.name + " with executor " + executor.toString)
      executor.asInstanceOf[CommandExecutor[Any]].execute(command)

    }

    case _ =>{
      logError("ActionExecutingActor  wrong message " )
    }
  }
}

class CommandExecutingActor(val executors: List[CommandExecutor[_]])(implicit inj: Injector) extends Actor with Logging {



  override def receive: Actor.Receive = {

    case CommandHandler(command) => {
      logInfo("CommandExecutingActor: " + command.name )
      val filtered = executors.filter(exec => exec.acceptCommand(command))
      logInfo("found : " + filtered.size + " suitable executors")
      val commandHandler = new CommandHandler(command)
      filtered.foreach(exec => {
        val executingProps: Props = Props(classOf[ActionExecutingActor],exec)
        val actionActor =context.actorOf(executingProps)
        actionActor ! commandHandler
      })


    }

    case _ =>{
      logError("CommandExecutingActor  wrong message " )
    }
  }
}

class CommandPersistActor(implicit inj: Injector) extends Actor with Logging {



  override def receive: Actor.Receive = {
    case CommandHandler(command) => {
      logInfo("Persisting: " + command.name)


      val execActor = injectActorRef[CommandExecutingActor]
      execActor ! new CommandHandler(command)


    }

    case _ =>{
      logError("CommandPersistActor  wrong message " )
    }
  }
}


class CommandHandlerActor(implicit inj: Injector) extends Actor with Logging {


  //val validatingProps: Props =

  // val eventPersistProps:Props = Props(classOf[CommandPersistActor],executors)

  override def receive: Actor.Receive = {

    case CommandHandler(command) => {
      logInfo("Evaluating : "  + command.name)
      val validatingActor = injectActorRef[CommandValidatingActor]
      implicit val timeout = Timeout(5 seconds)
      val future = validatingActor ? new CommandHandler(command)
      val result:Either[String,String] = Await.result(future, timeout.duration).asInstanceOf[Either[String,String]]
      result match {
        case Right(out) => {
          val persistActor = injectActorRef[CommandPersistActor]
          persistActor ! new CommandHandler(command)

        }
        case Left(_) => println("Error")
      }
      sender ! result

    }

  }
}
