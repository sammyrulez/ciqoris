package ciqoris

import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.{ Props, Actor, ActorSystem }
import org.junit.runner.RunWith

import org.scalatest.WordSpecLike
import org.scalatest.junit.JUnitRunner

import akka.util.Timeout
import scala.concurrent.Await
import scala.util.{ Success, Failure }
import scala.concurrent.duration._
import akka.pattern.ask

import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class ActorTest  extends TestKit(ActorSystem("testsystem"))
with WordSpecLike
with ImplicitSender
with StopSystemAfterAll {

  "An CommandHandlerActor" must {
    "Handle commands" in {
      implicit val timeout = Timeout(5 seconds)
      implicit val ec = system.dispatcher
      val validators:List[CommandValidator[_]] = List(new DummyCommandValidator())
      val commandExecutor: DummyCommandExecutor = new DummyCommandExecutor("empty")
      val executors:List[CommandExecutor[_]] = List(commandExecutor)
      val handlerProps = Props(classOf[CommandHandlerActor],validators,executors)
      val commandActor = system.actorOf(handlerProps, "commandActor")
      val future = commandActor ? new CommandHandler[String](new Command[String]("task","tast","payload"))
      future.onComplete {
        case Failure(_)   => fail()
        case Success(msg) => println(msg)
      }

      Await.ready(future, timeout.duration)

    }
  }

}
