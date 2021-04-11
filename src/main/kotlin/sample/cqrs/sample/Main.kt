package sample.cqrs.sample

import arrow.core.traverseEither
import sample.cqrs.sample.cart.*
import sample.cqrs.sample.cart.read.FullCartReadModel
import sample.cqrs.sample.cart.read.OrderConfirmedReadModel
import sample.cqrs.sample.cart.write.cartWriteDomain
import sample.cqrs.sample.core.commandProcessor
import sample.cqrs.sample.core.open
import sample.cqrs.sample.core.multipleReadEventHandler


fun main() {
    val store = CartEventStore()
    val fullCartReadModel = FullCartReadModel(mapOf("product1" to 7.99, "product2" to 23.47))
    val orderReadModel = OrderConfirmedReadModel()

    val cartWriteDomain = cartWriteDomain(store)
    val cartReadEventHandler = multipleReadEventHandler(listOf(fullCartReadModel::handleEvent, orderReadModel::handleEvent.open()))

    val cartCQRSDomain = commandProcessor(cartWriteDomain, cartReadEventHandler)

    listOf(
        CreateCartCommand(cartId = "cart1"),
        SelectProductCommand(cartId = "cart1", productId = "product1", quantity = 2),
        SelectProductCommand(cartId = "cart1", productId = "product1", quantity = 3),
        CreateCartCommand(cartId = "cart2"),
        SelectProductCommand(cartId = "cart1", productId = "product2", quantity = 2),
        UnselectProductCommand(cartId = "cart1", productId = "product2", quantity = 1),
        SelectProductCommand(cartId = "cart2", productId = "product1", quantity = 4),
        ConfirmOrderCommand(cartId = "cart1")
    ).reversed() // Because traverse use foldRight o_O
        .traverseEither(cartCQRSDomain)
        .map { it.reversed() }
        .fold(
            { e -> println("Error processing commands => $e")},
            { events -> println("Generated events : $events")}
        )

    println("Projection full cart for cart1 : ${fullCartReadModel.getProjection("cart1")}")
    println("Projection full cart for cart2 : ${fullCartReadModel.getProjection("cart2")}")

    println("Projection confirmed order : ${orderReadModel.getProjection()}")
}