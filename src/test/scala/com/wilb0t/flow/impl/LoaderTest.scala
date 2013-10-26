package com.wilb0t.flow.impl

import com.wilb0t.flow.api._

import org.scalatest.FunSuite
import com.weiglewilczek.slf4s.Logging

class LoaderTest extends FunSuite with Logging {

  test("simple load") {
    val loader = new Loader();

    val flowStr = """
[ Flow : "test flow"
  [ ActionNode : "headNode"
    com.wilb0t.flow.impl.PassAction
    [ com.wilb0t.flow.impl.PassExit "MidNode" ]
    [ com.wilb0t.flow.impl.FailExit "EndNode" ]
  ]
  [ ActionNode : "MidNode"
    com.wilb0t.flow.impl.FailAction
    [ com.wilb0t.flow.impl.PassExit "EndNode" ]
    [ com.wilb0t.flow.impl.FailExit "EndNode" ]
  ]
  [ EndNode : "endNode"
    com.wilb0t.flow.impl.PassAction 
  ]
]
"""

    val flow = loader.load(flowStr)

    logger.info("got flow: "+flow)
  }
}

