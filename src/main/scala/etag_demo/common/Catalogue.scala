package etag_demo.common

import zio.schema.{DeriveSchema, Schema}
import zio.schema.annotation.description

case class Catalogue(
  @description("Version of the catalogue")
  version: Int,
  @description("The available items and their prices")
  items: Map[String, Int]
)

object Catalogue {
  implicit val schema: Schema[Catalogue] = DeriveSchema.gen
}
