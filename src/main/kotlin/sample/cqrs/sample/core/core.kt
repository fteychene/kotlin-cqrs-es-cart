package sample.cqrs.sample.core

import arrow.core.Either
import arrow.core.flatMap

typealias WriteDomain<COMMAND, ERROR, EVENT> = (COMMAND) -> Either<ERROR, List<EVENT>>

typealias CommandHandler<COMMAND, ERROR, EVENT> = (COMMAND) -> Either<ERROR, List<EVENT>>

typealias EventHandler<EVENT, ERROR> = (List<EVENT>) -> Either<ERROR, List<EVENT>>

typealias ReadEventHandler<EVENT> = (EVENT) -> Unit // Send event to read model is a fire & forget action

fun <COMMAND, ERROR, EVENT> writeDomain(
    commandHandler: CommandHandler<COMMAND, ERROR, EVENT>,
    eventHandler: EventHandler<EVENT, ERROR>
): WriteDomain<COMMAND, ERROR, EVENT> = { command ->
    commandHandler(command)
        .flatMap(eventHandler)
}

fun <COMMAND, ERROR, EVENT> commandProcessor(writeDomain: WriteDomain<COMMAND, ERROR, EVENT>, readHandler: ReadEventHandler<EVENT>): (COMMAND) -> Either<ERROR, List<EVENT>> = { command: COMMAND ->
    writeDomain(command)
        .map { events ->
            events.forEach(readHandler)
            events
        }
}

fun <EVENT> multipleReadEventHandler(handlers: List<ReadEventHandler<EVENT>>): ReadEventHandler<EVENT> = { event ->
     handlers.map { h -> h(event) }
}

inline fun <ORIGIN, reified TARGET: ORIGIN> ReadEventHandler<TARGET>.open(): ReadEventHandler<ORIGIN> = { e: ORIGIN ->
    when(e) {
        is TARGET -> this(e)
        else -> {}
    }
}