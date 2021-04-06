package sample.cqrs.foodcart

import arrow.core.traverseEither
import sample.cqrs.foodcart.command.cartCommand
import sample.cqrs.foodcart.core.ConfirmOrderCommand
import sample.cqrs.foodcart.core.CreateCartCommand
import sample.cqrs.foodcart.core.SelectProductCommand
import sample.cqrs.foodcart.core.UnselectProductCommand
import sample.cqrs.foodcart.query.CartEventStore
import sample.cqrs.foodcart.query.FullCartProjection
import sample.cqrs.foodcart.query.OrderConfirmedProjection

fun main() {
    val store = CartEventStore()

    val foodCart1Id = "cart1"
    val foodCart2Id = "cart2"
    val product1Id = "product1"
    val product2Id = "product2"

    val projections = listOf(
        FullCartProjection(mapOf(product1Id to 7.99, product2Id to 23.47)),
        OrderConfirmedProjection()
    )

    listOf(
        CreateCartCommand(cartId = foodCart1Id),
        SelectProductCommand(cartId = foodCart1Id, productId = product1Id, quantity = 2),
        SelectProductCommand(cartId = foodCart1Id, productId = product1Id, quantity = 3),
        CreateCartCommand(cartId = foodCart2Id),
        SelectProductCommand(cartId = foodCart1Id, productId = product2Id, quantity = 2),
        UnselectProductCommand(cartId = foodCart1Id, productId = product2Id, quantity = 1),
        SelectProductCommand(cartId = foodCart2Id, productId = product1Id, quantity = 4),
        ConfirmOrderCommand(cartId = foodCart1Id)
    ).reversed() // Because traverse use foldRight o_O
        .traverseEither { cartCommand(it, store, projections) }
        .map { it.reversed() }
        .fold(
            { e -> println("Error processing commands => $e")},
            { events -> println("Generated events : $events")}
        )

    projections
        .forEach {
            when(it) {
                is FullCartProjection -> {
                    println("Projection full cart for $foodCart1Id : ${it.getProjection(foodCart1Id)}")
                    println("Projection full cart for $foodCart2Id : ${it.getProjection(foodCart2Id)}")
                }
                is OrderConfirmedProjection -> println("Projection confirmed order : ${it.getProjection()}")
            }
        }

}