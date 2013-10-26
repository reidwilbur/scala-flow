package com.wilb0t.flow.impl

import com.wilb0t.flow.api._

import scala.util.parsing.combinator._
import com.weiglewilczek.slf4s.Logging

class FlowParser extends JavaTokenParsers {

  def exitPort: Parser[ExitPort] = 
    """[a-zA-Z_][\w\.]*""".r ^^ 
    (ep => Class.forName(ep).newInstance.asInstanceOf[ExitPort] )

  def nextNode: Parser[String] = stringLiteral

  def nodeExit: Parser[(ExitPort,String)] = 
    "["~> exitPort~nextNode <~"]" ^^
    { case ep~name => (ep, name) }
  
  def action: Parser[Action] = 
    """[a-zA-Z_][\w\.]*""".r ^^ 
    (a => Class.forName(a).newInstance.asInstanceOf[Action] )

  def nodeName: Parser[String] = stringLiteral

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

  def flowName: Parser[String] = stringLiteral

  def flow: Parser[Flow] = 
    "["~>"Flow"~>":"~> flowName~rep(actionNode|endNode) <~"]" ^^
    { case name~nodes => Flow(name, nodes) }
}

class Loader extends FlowLoader with Logging {
  override def load(flowString: String): Flow = {
    val parser = new FlowParser()

    val parseResult = parser.parseAll(parser.flow, flowString)

    logger.info("parsed\n"+parseResult.get)

    parseResult.get
  }
}
