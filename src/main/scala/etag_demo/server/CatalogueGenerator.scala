package etag_demo.server

import etag_demo.common.Catalogue

object CatalogueGenerator {
  private val hardcodedItems: Seq[(String, Int)] = Seq(
    "Pizza Margherita" -> 1500,
    "Cheeseburger" -> 1200,
    "Spaghetti Carbonara" -> 1800,
    "Caesar Salad" -> 1600,
    "Club Sandwich" -> 1400,
    "Chicken Soup" -> 800,
    "Grilled Sirloin Steak" -> 3500,
    "Fish and Chips" -> 2200,
    "Sushi Platter" -> 4000,
    "Tonkotsu Ramen" -> 2500,
    "Chicken Curry" -> 1900,
    "Beef Tacos" -> 1200,
    "Loaded Nachos" -> 1300,
    "Charcuterie Board" -> 3000,
    "Gelato Trio" -> 900,
    "Chocolate Lava Cake" -> 1100,
    "Apple Pie" -> 900,
    "Bruschetta" -> 800,
    "Garlic Bread" -> 700,
    "Goulash" -> 1700,
    "Beef Stroganoff" -> 2600,
    "Pierogi" -> 1300,
    "Ratatouille" -> 1500,
    "Fried Chicken" -> 2500,
    "Vegetable Stir Fry" -> 1600
  )

  val size = hardcodedItems.size

  def data(round: Int): Catalogue = {
    assert(round > 0)
    val version = Math.min(hardcodedItems.size, round)
    val inflationRate: Double = 1 + (version / 5) * 0.1
    val itemsInThisRound = hardcodedItems.take(version).map(pair => (pair._1, (pair._2 * inflationRate).toInt))

    Catalogue(version, items = itemsInThisRound.toMap)
  }
}
