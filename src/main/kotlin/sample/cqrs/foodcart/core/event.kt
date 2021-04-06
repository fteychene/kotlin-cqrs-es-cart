package sample.cqrs.foodcart.core

sealed class CartEvent(open val cartId: CartId)

data class CartCreatedEvent(override val cartId: CartId): CartEvent(cartId)

data class ProductSelectedEvent(
    override val cartId: CartId,
    val productId: ProductId,
    val quantity: Int
): CartEvent(cartId)

data class ProductUnselectedEvent(
    override val cartId: CartId,
    val productId: ProductId,
    val quantity: Int
): CartEvent(cartId)

data class OrderConfirmedEvent(override val cartId: CartId): CartEvent(cartId)