package etag_demo.common

import zio.schema.{DeriveSchema, Schema}

case class Catalogue(version: Int, items: Map[String, Int])

object Catalogue {
  implicit val schema: Schema[Catalogue] = DeriveSchema.gen
}
