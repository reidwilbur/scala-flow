package com.wilb0t.flow.api

case class Flow(val name: String, val nodes: List[Node]) {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))
}

trait FlowRunner {
  def execute(flow: Flow, context: FlowContext, subContexts: List[FlowContext]): List[NodeResult]
}

trait FlowLoader {
  def load(flowString: String): Flow
  def load(flowFile: java.io.File): Flow
}
