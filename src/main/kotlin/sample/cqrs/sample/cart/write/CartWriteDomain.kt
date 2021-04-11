package sample.cqrs.sample.cart.write

import arrow.core.Either
import arrow.core.filterOrElse
import arrow.core.left
import arrow.core.right
import sample.cqrs.sample.cart.*
import sample.cqrs.sample.core.*

sealed class CartDomainException
data class HandlerException(val cause: CommandHandlerException): CartDomainException()
data class StoreException(val cause: CartStoreException): CartDomainException()

fun cartCommmandHandler(store: CartEventStore): CommandHandler<CartCommand, CartDomainException, CartEvent> = { command ->
    when (command) {
        is CreateCartCommand -> handleCreateCartCommand(command, store).map(::listOf)
        is SelectProductCommand -> handleSelectProductCommand(command).map(::listOf)
        is UnselectProductCommand -> handleUnselectProductCommand(command, store).map(::listOf)
        is ConfirmOrderCommand -> handleConfirmOrderCommand(command, store).map { if (it != null) listOf(it) else listOf() }
    }.mapLeft(::HandlerException)
}

fun storeCartEvent(store: CartEventStore): EventHandler<CartEvent, CartDomainException> = { events ->
    store.addEvent(events)
        .mapLeft(::StoreException)
}

fun cartWriteDomain(store: CartEventStore): WriteDomain<CartCommand, CartDomainException, CartEvent> =
    writeDomain(cartCommmandHandler(store), storeCartEvent(store))

data class Cart(
    val id: CartId,
    val products: Map<ProductId, Int> = mapOf(),
    val confirmed: Boolean = false
)

sealed class CommandHandlerException
data class CartAlreadyExist(val aggregateId: CartId) : CommandHandlerException()
data class InvalidFoodCartAggregate(val aggregateId: CartId, val cause: CartStoreException) : CommandHandlerException()
data class ProductDeselectionException(val message: String) : CommandHandlerException()

fun handleCreateCartCommand(command: CreateCartCommand, eventStore: CartEventStore): Either<CommandHandlerException, CartCreatedEvent> =
    if (eventStore.loadAggregateById(command.cartId).isRight()) CartCreatedEvent(cartId = command.cartId).right()
    else CartAlreadyExist(command.cartId).left()

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
        .map {
            ProductUnselectedEvent(
                cartId = command.cartId,
                productId = command.productId,
                quantity = command.quantity
            )
        }

fun handleConfirmOrderCommand(command: ConfirmOrderCommand, eventStore: CartEventStore): Either<CommandHandlerException, OrderConfirmedEvent?> =
    eventStore.loadAggregateById(command.cartId)
        .mapLeft { InvalidFoodCartAggregate(command.cartId, it) }
        .map {
            if (it.confirmed) null
            else OrderConfirmedEvent(cartId = command.cartId)
        }

fun onCartCreatedEvent(event: CartCreatedEvent): Cart =
    Cart(id = event.cartId)

fun onProductSelectedEvent(cart: Cart, event: ProductSelectedEvent): Cart =
    cart.copy(products = cart.products + (event.productId to cart.products.getOrDefault(event.productId, 0) + event.quantity))

fun onProductUnselectedEvent(cart: Cart, event: ProductUnselectedEvent): Cart =
    cart.copy(products = cart.products + (event.productId to cart.products.getOrDefault(event.productId, 0) - event.quantity))

fun onOrderConfirmedEvent(cart: Cart, event: OrderConfirmedEvent): Cart =
    cart.copy(confirmed = true)

fun reload(events: List<CartEvent>): Cart =
    events.fold(Cart(id = "")) { foodCart, event ->
        when (event) {
            is CartCreatedEvent -> onCartCreatedEvent(event)
            is OrderConfirmedEvent -> onOrderConfirmedEvent(foodCart, event)
            is ProductSelectedEvent -> onProductSelectedEvent(foodCart, event)
            is ProductUnselectedEvent -> onProductUnselectedEvent(foodCart, event)
        }
    }