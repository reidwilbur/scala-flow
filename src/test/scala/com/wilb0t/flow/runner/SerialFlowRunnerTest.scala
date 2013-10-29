package com.wilb0t.flow.runner

import com.wilb0t.flow.api._

import com.wilb0t.flow.impl.PassAction
import com.wilb0t.flow.impl.PassExit
import com.wilb0t.flow.impl.FailAction
import com.wilb0t.flow.impl.FailExit

import org.scalatest.FunSuite
import com.weiglewilczek.slf4s.Logging

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SerialFlowTest extends FunSuite with Logging {

  val passAction = new PassAction()
  val failAction = new FailAction()

  test("simple flow") {

    val flow = 
      Flow("TestFlow", List[Node](
        ActionNode("HeadNode", passAction, {
          case PassExit() => Some("CompFlow")
          case _ => Some("EndNode")
        }),
        SubFlowNode("CompFlow", List[Node](
            EndNode("CompFlowEnd", passAction)
          ), {
            case PassExit() => Some("MidNode")
            case _ => Some("EndNode")
        }),
        ActionNode("MidNode", failAction, {
          case PassExit() => Some("SubFlow")
          case FailExit() => Some("SubFlow")
          case _ => Some("EndNode")
        }),
        ParSubFlowNode("SubFlow", List[Node](
            ActionNode("SubFlowHead", passAction, {
              case _ => Some("SubFlowEndNode")
            }),
            EndNode("SubFlowEndNode", failAction)
            ),
            "EndNode"
        ),
        EndNode("EndNode", passAction)
        )
      )

    val flowRunner = new SerialFlowRunner()
    val results = flowRunner.execute(flow, FlowContext("Main"), List(FlowContext("Ctx1"), FlowContext("Ctx2")))

    logger.info("Got Results: "+results)

    assert(results.size == 10)

    assert(results(0).node.name == "HeadNode")
    assert(results(0).context == FlowContext("Main"))
    assert(results(0).exitPort == PassExit())

    assert(results(1).node.name == "CompFlowEnd")
    assert(results(1).context == FlowContext("Main"))
    assert(results(1).exitPort == PassExit())

    assert(results(2).node.name == "CompFlow")
    assert(results(2).context == FlowContext("Main"))
    assert(results(2).exitPort == PassExit())

    assert(results(3).node.name == "MidNode")
    assert(results(3).context == FlowContext("Main"))
    assert(results(3).exitPort == FailExit())

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowHead" 
          && result.context == FlowContext("Ctx1") 
          && result.exitPort == PassExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowEndNode" 
          && result.context == FlowContext("Ctx1") 
          && result.exitPort == FailExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowHead" 
          && result.context == FlowContext("Ctx2") 
          && result.exitPort == PassExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowEndNode" 
          && result.context == FlowContext("Ctx2") 
          && result.exitPort == FailExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlow" 
          && result.context == FlowContext("Main") 
          && result.exitPort == ParSubFlowExit()
      )
    )

    assert(results.last.node.name == "EndNode")
    assert(results.last.context == FlowContext("Main"))
    assert(results.last.exitPort == PassExit())

  }
}

