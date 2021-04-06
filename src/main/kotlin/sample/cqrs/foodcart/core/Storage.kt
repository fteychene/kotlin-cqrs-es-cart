package sample.cqrs.foodcart.query

import arrow.core.*
import sample.cqrs.foodcart.command.*
import sample.cqrs.foodcart.core.*
import kotlin.collections.fold

class CartEventStore {
    private val datas: MutableMap<CartId, List<CartEvent>> = mutableMapOf()

    fun addEvent(cartId: CartId, events: List<CartEvent>): Either<IllegalStateException, List<CartEvent>> {
        datas[cartId] = datas.getOrDefault(cartId, listOf()) + events
        return events.right()
    }

    fun loadAggregateById(cartId: CartId): Either<IllegalStateException, Cart> =
        datas.getOrDefault(cartId, listOf())
            .fold(Cart(id = "")) { foodCart, event ->
                when (event) {
                    is CartCreatedEvent -> onFoodCartCreatedEvent(event)
                    is OrderConfirmedEvent -> onOrderConfirmedEvent(foodCart, event)
                    is ProductSelectedEvent -> onProductSelectedEvent(foodCart, event)
                    is ProductUnselectedEvent -> onProductUnselectedEvent(foodCart, event)
                }
            }.right()


}