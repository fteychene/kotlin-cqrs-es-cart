package sample.cqrs.foodcart.query

import sample.cqrs.foodcart.core.*

data class FullCart(
    val id: CartId = "",
    val products: Map<ProductId, Int> = mapOf(),
    val price: Double = 0.0,
    val confirmed: Boolean = false
)

class FullCartProjection(
    private val prices: Map<ProductId, Double>
): Projection<CartEvent> {

    val datas: MutableMap<CartId, FullCart> = mutableMapOf()

    fun computePrice(cart: FullCart): FullCart =
        cart.copy(price = cart.products.map { (key, value) -> prices[key]!!*value }.sum())

    override fun handleEvent(event: CartEvent) {
        val t = datas.getOrDefault(event.cartId, FullCart())
        val latest = when(event) {
            is CartCreatedEvent -> t.copy(id = event.cartId)
            is OrderConfirmedEvent -> t.copy(confirmed = true)
            is ProductSelectedEvent -> computePrice(t.copy(products = t.products + (event.productId to t.products.getOrDefault(event.productId, 0) + event.quantity)))
            is ProductUnselectedEvent -> computePrice(t.copy(products = t.products + (event.productId to t.products.getOrDefault(event.productId, 0) - event.quantity)))
        }
        datas[event.cartId] = latest
    }

    fun getProjection(id: CartId): FullCart? = datas[id]

}

class OrderConfirmedProjection: Projection<CartEvent> {
    private var counter = 0

    override fun handleEvent(event: CartEvent) {
        if (event is OrderConfirmedEvent) {
            counter += 1
        }
    }

    fun getProjection(): Int = counter
}