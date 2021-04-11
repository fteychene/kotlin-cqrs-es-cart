package sample.cqrs.sample.cart

import arrow.core.*
import sample.cqrs.sample.cart.write.Cart
import sample.cqrs.sample.cart.write.reload

sealed class CartStoreException

class CartEventStore {
    private val datas: MutableMap<CartId, List<CartEvent>> = mutableMapOf()

    fun addEvent(events: List<CartEvent>): Either<CartStoreException, List<CartEvent>> {
        events.forEach { event ->
            datas[event.cartId] = datas.getOrDefault(event.cartId, listOf()) + event
        }
        return events.right()
    }

    fun loadAggregateById(cartId: CartId): Either<CartStoreException, Cart> =
        reload(datas.getOrDefault(cartId, listOf())).right()
}