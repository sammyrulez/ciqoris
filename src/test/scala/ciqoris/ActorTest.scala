package ciqoris

import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.{ Props, Actor, ActorSystem }
import org.junit.runner.RunWith

import org.scalatest.WordSpecLike
import org.scalatest.junit.JUnitRunner

import akka.util.Timeout
import scaldi.{Module, Injector}
import scala.concurrent.Await
import scala.util.{ Success, Failure }
import scala.concurrent.duration._
import akka.pattern.ask
import scaldi.akka.AkkaInjectable._

import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class ActorTest  extends TestKit(ActorSystem("testsystem"))
with WordSpecLike
with ImplicitSender
with StopSystemAfterAll {

  val diModule: Module with Object = new Module {
    bind[ActorSystem] to system

    binding toProvider new CommandHandlerActor
    binding toProvider new CommandValidatingActor(List())
    binding toProvider new CommandPersistActor(List())
    binding toProvider new CommandExecutingActor(List())

  }

  "An CommandHandlerActor" must {
    "Handle commands" in {
      implicit val timeout = Timeout(5 seconds)
      //implicit val system = inject [ActorSystem]
      implicit val ec = system.dispatcher

      val validators:List[CommandValidator[_]] = List(new DummyCommandValidator())
      val commandExecutor: DummyCommandExecutor = new DummyCommandExecutor("empty")
      val executors:List[CommandExecutor[_]] = List(commandExecutor)


      implicit val appModule: Injector = diModule

      //val handlerProps = Props(classOf[CommandHandlerActor],validators,executors)
      val commandActor = injectActorRef[CommandHandlerActor]
      val future = commandActor ? new CommandHandler[String](new Command[String]("task","tast","payload"))
      future.onComplete {
        case Failure(_)   => fail()
        case Success(msg) => println(msg)
      }

      Await.ready(future, timeout.duration)

    }
  }

}
