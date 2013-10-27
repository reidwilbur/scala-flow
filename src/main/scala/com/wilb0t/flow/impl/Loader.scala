package com.wilb0t.flow.impl

import com.wilb0t.flow.api._

import scala.util.parsing.combinator._
import com.weiglewilczek.slf4s.Logging

class FlowParser(val flowDir: java.io.File) extends JavaTokenParsers with Logging {

  def mkExitPortFn(exitPorts: List[(Option[ExitPort],String)]): ExitPort => Option[String] = {
    val nodeExitMap: Map[ExitPort,String] = 
      Map() ++ exitPorts
                 .withFilter{ case (Some(ep), _) => true; case _ => false }
                 .map{ case (Some(ep), nodeName) => (ep -> nodeName) }

    //val default: Option[String] = 
    //  exitPorts
    //    .withFilter{ case (None, nodeName) => true; _ => false }
    //    .map{ _._2 }
    //    .lastOption

    new Function1[ExitPort, Option[String]]{
      override def apply(ep: ExitPort): Option[String] = {
        //nodeExitMap.getOrElse(ep, default)
        nodeExitMap.get(ep)
      }
    }
  }

  def exitPort: Parser[ExitPort] = 
    """[a-zA-Z_][\w\.]*""".r ^^ 
    (ep => Class.forName(ep).newInstance.asInstanceOf[ExitPort] )


  def nextNode: Parser[String] = """\w+""".r


  def nodeExit: Parser[(Option[ExitPort],String)] = 
    "["~> opt(exitPort)~":"~nextNode <~"]" ^^
    { case ep~":"~name => (ep, name) }


  def action: Parser[Action] = 
    """[a-zA-Z_][\w\.]*""".r ^^ 
    (a => Class.forName(a).newInstance.asInstanceOf[Action] )


  def nodeName: Parser[String] = """\w+""".r


  def actionNode: Parser[ActionNode] = 
    "["~>"ActionNode"~>":"~> nodeName~action~nodeExit~rep(nodeExit) <~"]" ^^
    { 
      case name~action~exit~restexits => 
        //val nodeExitMap = Map() ++ (exit :: restexits).withFilter{ case (Some(ep), _) => true }
        ActionNode(name, action, mkExitPortFn(exit :: restexits) )
    }

  
  def endNode: Parser[EndNode] = 
    "["~>"EndNode"~>":"~> nodeName~action <~"]" ^^ 
    { case name~action => EndNode(name, action) }


  def subflowFileName: Parser[String] = """.+\.sf""".r


  def subflowNode: Parser[SubFlowNode] =
    "["~>"SubFlowNode"~>":"~> nodeName~subflowFileName~nodeExit~rep(nodeExit) <~"]" >>
    { case name~filename~exit~restexits =>
        val file = new java.io.File(flowDir, filename)
        logger.debug("Using subflow file "+file.toString)

        val rdr = new java.io.FileReader(file)

        try {
          //here we are recursively creating a flow parser
          //in the transformer for the subflow ref parser
          //
          //using the into >> operator allows us to dynamically chain
          //another parser (and its result) into this parser chain
          val subflowParser = new FlowParser(flowDir)
          val parseResult = subflowParser.parseAll(subflowParser.flow, rdr)
          logger.debug(parseResult.toString)

          //create an anonymous parser based on the subflow file parsing
          //its apply method isn't based on the input passed to it
          //since we have already parse the file
          //return a ParseResult based on the status of the overall subflow parse
          new Parser[SubFlowNode]{ 
            override def apply(in: Input): ParseResult[SubFlowNode] = {
              parseResult.successful match {
                case true =>
                  //unpack the flow nodes and create the subflow object
                  val flow = parseResult.get
                  val subflow = SubFlowNode(name, flow.nodes, mkExitPortFn(exit :: restexits) )
                  Success(subflow, in)
                case false =>
                  Failure("Failed to parse "+filename, in)
              }
            }
          }
        }
        finally {
          rdr.close
        }
    }


  def parSubflowNode: Parser[ParSubFlowNode] = 
    "["~>"ParSubFlowNode"~>":"~> nodeName~subflowFileName~nextNode <~"]" >>
    { case name~filename~next =>
        val file = new java.io.File(flowDir, filename)
        logger.debug("Using subflow file "+file.toString)

        val rdr = new java.io.FileReader(file)

        try {
          val subflowParser = new FlowParser(flowDir)
          val parseResult = subflowParser.parseAll(subflowParser.flow, rdr)
          logger.debug(parseResult.toString)

          new Parser[ParSubFlowNode]{ 
            override def apply(in: Input): ParseResult[ParSubFlowNode] = {
              parseResult.successful match {
                case true =>
                  val flow = parseResult.get
                  val subflow = ParSubFlowNode(name, flow.nodes, next)
                  Success(subflow, in)
                case false =>
                  Failure("Failed to parse "+filename, in)
              }
            }
          }
        }
        finally {
          rdr.close
        }
    }


  def flowName: Parser[String] = """\w+""".r

  def flow: Parser[Flow] = 
    "["~>"Flow"~>":"~> flowName~rep(actionNode|subflowNode|parSubflowNode|endNode) <~"]" ^^
    { case name~nodes => Flow(name, nodes) }

}

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
