package sample.cqrs.foodcart.command

import arrow.core.Either
import arrow.core.filterOrElse
import arrow.core.left
import arrow.core.right
import sample.cqrs.foodcart.core.*
import sample.cqrs.foodcart.query.CartEventStore

data class Cart(
    val id: CartId,
    val products: Map<ProductId, Int> = mapOf(),
    val confirmed: Boolean = false
    )

sealed class CommandHandlerException
data class FoodCartAlreadyExist(val aggregateId: CartId): CommandHandlerException()
data class InvalidFoodCartAggregate(val aggregateId: CartId, val cause: IllegalStateException): CommandHandlerException()
data class ProductDeselectionException(val message: String): CommandHandlerException()

fun handleCreateFoodCartCommand(command: CreateCartCommand, eventStore: CartEventStore): Either<CommandHandlerException, CartCreatedEvent> =
    if (eventStore.loadAggregateById(command.cartId).isRight()) CartCreatedEvent(cartId = command.cartId).right()
    else FoodCartAlreadyExist(command.cartId).left()

fun handleSelectProductCommand(command: SelectProductCommand): Either<CommandHandlerException, ProductSelectedEvent> =
    ProductSelectedEvent(
        cartId = command.cartId,
        productId = command.productId,
        quantity = command.quantity
    ).right()

fun handleUnselectProductCommand(command: UnselectProductCommand, eventStore: CartEventStore): Either<CommandHandlerException, ProductUnselectedEvent> =
    eventStore.loadAggregateById(command.cartId)
        .mapLeft { InvalidFoodCartAggregate(command.cartId, it) }
        .filterOrElse({ it.products.containsKey(command.productId) }) { ProductDeselectionException("Cannot deselect a product which has not been selected for this Food Cart") }
        .filterOrElse({ it.products[command.productId]!! - command.quantity > 0 }) { ProductDeselectionException("Cannot deselect more products of ID [" + command.productId + "] than have been selected initially") }
        .map { ProductUnselectedEvent(
                cartId = command.cartId,
                productId = command.productId,
                quantity = command.quantity
            ) }

fun handleConfirmOrderCommand(command: ConfirmOrderCommand, eventStore: CartEventStore): Either<CommandHandlerException, OrderConfirmedEvent?> =
    eventStore.loadAggregateById(command.cartId)
        .mapLeft { InvalidFoodCartAggregate(command.cartId, it) }
        .map {
            if (it.confirmed) null
            else OrderConfirmedEvent(cartId = command.cartId)
        }

fun onFoodCartCreatedEvent(event: CartCreatedEvent): Cart =
    Cart(id = event.cartId)

fun onProductSelectedEvent(cart: Cart, event: ProductSelectedEvent): Cart =
    cart.copy(products = cart.products + (event.productId to cart.products.getOrDefault(event.productId, 0) + event.quantity))

fun onProductUnselectedEvent(cart: Cart, event: ProductUnselectedEvent): Cart =
    cart.copy(products = cart.products + (event.productId to cart.products.getOrDefault(event.productId, 0) - event.quantity))

fun onOrderConfirmedEvent(cart: Cart, event: OrderConfirmedEvent): Cart =
    cart.copy(confirmed = true)