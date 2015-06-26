package ciqoris

import org.slf4j.LoggerFactory

/**
 * Created by sam on 26/06/15.
 */
trait Logging {

  val logger = LoggerFactory.getLogger(this.getClass)

  def logDebug(input:Any): Unit ={
    logger.debug(input.toString)
  }

  def logInfo(input:Any): Unit ={
    logger.info(input.toString)
  }
  def logWarn(input:Any): Unit ={
    logger.warn(input.toString)
  }
  def logError(input:Any): Unit ={
    logger.error(input.toString)
  }


}
