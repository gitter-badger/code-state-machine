package com.laamella.code_state_machine.builder

import com.laamella.code_state_machine.{Transition, StateMachine}
import grizzled.slf4j.Logger

import scala.collection.mutable

/**
  * A builder that can be used to create more complex builders.
  */
class StateMachineBuilder[State, Event, Priority <: Ordered[Priority]] {
  private lazy val log = Logger(getClass)

  private val startStates = mutable.Set[State]()
  private val endStates = mutable.Set[State]()
  private val exitEvents = mutable.Map[State, Seq[() => Unit]]()
  private val entryEvents = mutable.Map[State, Seq[() => Unit]]()
  private val transitions = mutable.Map[State, mutable.PriorityQueue[Transition[State, Event, Priority]]]()

  /** Adds a start state, and immediately activates it. */
  def addStartState(startState: State): Unit = {
    log.debug(s"Add start state '$startState'")
    startStates += startState
  }

  /** Add 0 or more actions to be executed when the state is exited. */
  def addExitActions(state: State, actions: Seq[() => Unit]): Unit = {
    log.debug(s"Create exit action for '$state' ($actions)")
    // TODO there's probably a better way to express this
    if (!exitEvents.contains(state)) {
      exitEvents += state -> actions
      return
    }
    exitEvents(state) ++= actions
  }

  /** Add 0 or more actions to be executed when the state is entered. */
  def addEntryActions(state: State, actions: Seq[() => Unit]): Unit = {
    log.debug(s"Create entry action for '$state' ($actions)")
    if (!entryEvents.contains(state)) {
      entryEvents += state -> actions
      return
    }
    entryEvents(state) ++= actions
  }

  /** Add an end state. */
  def addEndState(endState: State): Unit = {
    log.debug(s"Add end state '$endState'")
    endStates += endState
  }

  /** Add a transition. */
  def addTransition(transition: Transition[State, Event, Priority]): Unit = {
    val sourceState = transition.sourceState
    log.debug(s"Create transition from '$sourceState' to '${transition.destinationState}' (pre: '${transition.conditions}', action: '${transition.actions}')")
    if (!transitions.contains(sourceState)) {
      transitions.put(sourceState, mutable.PriorityQueue[Transition[State, Event, Priority]]())
    }
    transitions(sourceState) += transition
  }

  def build(): StateMachine[State, Event, Priority] = {
    val immutableStartStates = startStates.toSet
    val immutableEndStates = endStates.toSet
    val immutableEntryEvents = entryEvents.toMap
    val immutableExitEvents = exitEvents.toMap
    val immutableTransitions: Map[State, Seq[Transition[State, Event, Priority]]] = transitions.mapValues(transitionQueue => transitionQueue.toSeq).toMap
    new StateMachine[State, Event, Priority](immutableStartStates, immutableEndStates, immutableExitEvents, immutableEntryEvents, immutableTransitions)
  }
}
