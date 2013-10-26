package com.wilb0t.flow.impl

import com.wilb0t.flow.api._

import scala.util.parsing.combinator._
import com.weiglewilczek.slf4s.Logging

class FlowParser(val flowDir: java.io.File) extends JavaTokenParsers with Logging {

  def exitPort: Parser[ExitPort] = 
    """[a-zA-Z_][\w\.]*""".r ^^ 
    (ep => Class.forName(ep).newInstance.asInstanceOf[ExitPort] )

  def nextNode: Parser[String] = """\w+""".r

  def nodeExit: Parser[(ExitPort,String)] = 
    "["~> exitPort~nextNode <~"]" ^^
    { case ep~name => (ep, name) }
  
  def action: Parser[Action] = 
    """[a-zA-Z_][\w\.]*""".r ^^ 
    (a => Class.forName(a).newInstance.asInstanceOf[Action] )

  def nodeName: Parser[String] = """\w+""".r

  def actionNode: Parser[ActionNode] = 
    "["~>"ActionNode"~>":"~> nodeName~action~nodeExit~rep(nodeExit) <~"]" ^^
    { 
      case name~action~exit~restexits => 
        val nodeExitMap = Map() ++ (exit :: restexits)
        ActionNode(name, action, { nodeExitMap.get(_) })
    }
  
  def endNode: Parser[EndNode] = 
    "["~>"EndNode"~>":"~> nodeName~action <~"]" ^^ 
    { case name~action => EndNode(name, action) }

  def subflowFileName: Parser[String] = """.+\.sf""".r

  def subflowNode: Parser[SubFlowNode] =
    "["~>"SubFlowNode"~>":"~> nodeName~subflowFileName~nodeExit~rep(nodeExit) <~"]" ^^
    { 
      case name~filename~exit~restexits =>
        val file = new java.io.File(flowDir, filename)
        logger.info("Using subflow file "+file.toString)
        val rdr = new java.io.FileReader(file)
        try {
          val subflowParser = new FlowParser(flowDir)
          val parseResult = subflowParser.parseAll(subflowParser.flow, rdr)
          val flow = parseResult.get
          val exitMap = Map() ++ (exit :: restexits)
          SubFlowNode(name, flow.nodes, { exitMap.get(_) })
        }
        finally {
          rdr.close
        }
    }

  def flowName: Parser[String] = """\w+""".r

  def flow: Parser[Flow] = 
    "["~>"Flow"~>":"~> flowName~rep(actionNode|subflowNode|endNode) <~"]" ^^
    { case name~nodes => Flow(name, nodes) }
}

class Loader(val flowDir: java.io.File) extends FlowLoader with Logging {
  override def load(flowString: String): Flow = {
    val parser = new FlowParser(flowDir)

    val parseResult = parser.parseAll(parser.flow, flowString)

    logger.debug("parsed\n"+parseResult.get)

    parseResult.get
  }

  override def load(flowFile: java.io.File): Flow = {
    val parser = new FlowParser(flowDir)
    logger.info("Using file "+flowFile.toString)
    val rdr = new java.io.FileReader(flowFile)

    try {
      val parseResult = parser.parseAll(parser.flow, rdr)

      logger.info("parsed\n"+parseResult.get)

      parseResult.get
    }
    finally {
      rdr.close
    }
  }
}
