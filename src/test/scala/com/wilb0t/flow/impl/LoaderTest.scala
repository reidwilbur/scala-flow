package com.wilb0t.flow.impl

import com.wilb0t.flow.api._

import org.scalatest.FunSuite
import com.weiglewilczek.slf4s.Logging

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class LoaderTest extends FunSuite with Logging {

  test("simple load") {
    val loader = new Loader(new java.io.File(getClass.getResource("/").toURI))

    val flowStr = """
[ Flow : Test_Flow
  [ ActionNode : headNode
    com.wilb0t.flow.impl.PassAction
    [ com.wilb0t.flow.impl.PassExit : midNode ]
    [ com.wilb0t.flow.impl.FailExit : endNode ]
    [ : endNode ]
  ]
  [ ActionNode : midNode
    com.wilb0t.flow.impl.FailAction
    [ com.wilb0t.flow.impl.PassExit : endNode ]
    [ com.wilb0t.flow.impl.FailExit : endNode ]
    [ : endNode ]
  ]
  [ EndNode : endNode
    com.wilb0t.flow.impl.PassAction 
  ]
]
"""

    val flow = loader.load(flowStr)

    logger.info("got flow: "+flow)
  }

  test("load from file with subflow refs") {
    //TODO: for some reason, using "/" doesn't give me the right path...
    val flowDir = new java.io.File(getClass.getResource("/SimpleFlow.sf").toURI).getParentFile
    val loader = new Loader(flowDir)

    val loadResult = loader.load(new java.io.File(getClass.getResource("/SimpleFlow.sf").toURI))

    loadResult match {
      case Left(flow) => assert(true, flow.toString)
      case Right(msg) => assert(false, msg)
    }
  }

  test("load subflow") {
    //TODO: for some reason, using "/" doesn't give me the right path...
    val flowDir = new java.io.File(getClass.getResource("/SimpleFlow.sf").toURI).getParentFile
    val loader = new Loader(flowDir)

    val loadResult = loader.load(new java.io.File(getClass.getResource("/SimpleSubFlow.sf").toURI))

    loadResult match {
      case Left(flow) => assert(true, flow.toString)
      case Right(msg) => assert(false, msg)
    }
  }

  test("load par subflow") {
    //TODO: for some reason, using "/" doesn't give me the right path...
    val flowDir = new java.io.File(getClass.getResource("/SimpleFlow.sf").toURI).getParentFile
    val loader = new Loader(flowDir)

    val loadResult = loader.load(new java.io.File(getClass.getResource("/SimpleParSubFlow.sf").toURI))

    loadResult match {
      case Left(flow) => assert(true, flow.toString)
      case Right(msg) => assert(false, msg)
    }
  }
}

