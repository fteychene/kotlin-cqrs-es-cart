package sample.cqrs.foodcart.command

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import sample.cqrs.foodcart.core.*
import sample.cqrs.foodcart.query.CartEventStore

sealed class FoodCartException
data class CommandException(val message: String, val cause: CommandHandlerException) : FoodCartException()
data class StoreException(val message: String, val cause: IllegalStateException) : FoodCartException()

fun cartCommand(command: CartCommand, eventStore: CartEventStore, projections: List<Projection<CartEvent>>): Either<FoodCartException, List<CartEvent>> =
    when (command) {
        is CreateCartCommand -> handleCreateFoodCartCommand(command, eventStore)
        is ConfirmOrderCommand -> handleConfirmOrderCommand(command, eventStore)
        is SelectProductCommand -> handleSelectProductCommand(command)
        is UnselectProductCommand -> handleUnselectProductCommand(command, eventStore)
    }.mapLeft { e -> CommandException("Error processing command $command", e) }
        .flatMap {
            it?.let { event ->
                eventStore.addEvent(it.cartId, listOf(event))
                    .mapLeft { e -> StoreException("Error while adding event $event", e) }
            } ?: listOf<CartEvent>().right()
        }
        .flatMap { events ->
            events.map { event -> projections.forEach { projection -> projection.handleEvent(event) } };
            events.right()
        }