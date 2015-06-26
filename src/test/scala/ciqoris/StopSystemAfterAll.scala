package ciqoris

//<start id="ch02-stopsystem"/>
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

trait StopSystemAfterAll extends BeforeAndAfterAll { 
		//<co id="ch02-stop-system-before-and-after-all"/>
  this: TestKit with Suite => //<co id="ch02-stop-system-self-type"/>
  override protected def afterAll() {
    super.afterAll()
    system.shutdown() //<co id="ch02-stop-system-shutdown"/>
  }
}
//<end id="ch02-stopsystem"/>