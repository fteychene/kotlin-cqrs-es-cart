package sample.cqrs.sample.cart.read

import sample.cqrs.sample.cart.*


class OrderConfirmedReadModel{
    private var counter = 0

    fun handleEvent(event: OrderConfirmedEvent) {
        counter += 1
    }

    fun getProjection(): Int = counter
}