package com.wilb0t.flow.loader

import com.wilb0t.flow.api._

import com.wilb0t.flow.parser._

import com.weiglewilczek.slf4s.Logging

/** This Loader implementation uses the [[com.wilb0t.flow.parser.FlowParser]]
  * impl to instantiate [[com.wilb0t.flow.api.Flow]] objects from Strings
  * or Files.
  *
  * If the resulting object of the Either is a Flow, it is suitable to be
  * executed via a [[com.wilb0t.flow.api.FlowRunner]] implementation.
  *
  * Otherwise, the result of load is a String containing parsing error
  * associated with the input.
  */
class Loader(val flowDir: java.io.File) extends FlowLoader with Logging {
  override def load(flowString: String): Either[Flow, String] = {
    val parser = new FlowParser(flowDir)

    val parseResult = parser.parseAll(parser.flow, flowString)
    logger.debug("parsed: "+parseResult)
    
    parseResult match {
      case parser.Success(flow, _) => Left(flow)
      case parser.Error(msg, _) => Right(msg)
    }
  }

  override def load(flowFile: java.io.File): Either[Flow, String] = {
    val parser = new FlowParser(flowDir)
    logger.info("Using file "+flowFile.toString)
    val rdr = new java.io.FileReader(flowFile)

    try {
      val parseResult = parser.parseAll(parser.flow, rdr)
      logger.info("parsed: "+parseResult)
      
      parseResult match {
        case parser.Success(flow, _) => Left(flow)
        case parser.Error(msg, _) => Right(msg)
      }
    }
    catch {
      case ex: Exception => Right(ex.getMessage)
    }
    finally {
      rdr.close
    }
  }
}
