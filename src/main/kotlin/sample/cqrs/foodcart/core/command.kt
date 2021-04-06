package sample.cqrs.foodcart.core

typealias CartId = String
typealias ProductId = String

sealed class CartCommand

data class CreateCartCommand(val cartId: CartId): CartCommand()

data class SelectProductCommand(
    val cartId: CartId,
    val productId: ProductId,
    val quantity: Int
): CartCommand()

data class UnselectProductCommand(
    val cartId: CartId,
    val productId: ProductId,
    val quantity: Int
): CartCommand()

data class ConfirmOrderCommand(val cartId: CartId): CartCommand()

