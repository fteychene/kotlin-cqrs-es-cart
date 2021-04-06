package sample.cqrs.foodcart.core

// TODO reused these type of querying
//data class FindFoodCartQuery(val cartId: CartId)
//
//sealed class RetrieveProductQuery
//data class ById(val uuid: ProductId): RetrieveProductQuery()
//data class ByName(val name: String): RetrieveProductQuery()

interface Projection<E> {

    fun handleEvent(event: E)

    // TODO fun reload(id: I): A
}