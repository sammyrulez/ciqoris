package ciqoris

import java.util.{GregorianCalendar, Calendar}


class Audit(val source:String,val timeStamp:Calendar = new GregorianCalendar)

case class Command[T](val name:String,val id:String,val payload:T)

case class Event[T](val command: Command[T], val audit: Audit)

trait CommandReciver{

  def acceptCommand(command:Command[_]):Boolean

}

trait CommandValidator[T ] extends CommandReciver{

  def validate(command:Command[T ]):Either[String,String]

}

trait CommandExecutor[T]extends CommandReciver{

  def execute(command:Command[T]):Unit

}

trait EventPersister[T] {

  def persist( source:String, command:Command[T]) = {
    persistEvent(new Event[T](command,new Audit(source,new GregorianCalendar())))
  }

  def persistEvent(event:Event[T])

}
