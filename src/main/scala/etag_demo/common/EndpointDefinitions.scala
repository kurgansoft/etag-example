package etag_demo.common

import zio.ZNothing
import zio.http.codec.{HeaderCodec, HttpCodec, StatusCodec}
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.Endpoint
import zio.http.{Header, RoutePattern}

object EndpointDefinitions {
  val getCatalogue: Endpoint[Unit, Unit, ZNothing, Catalogue, None] = Endpoint(RoutePattern.GET / "catalogue")
    .outCodec(StatusCodec.Ok ++ HttpCodec.content[Catalogue])

  val getCatalogueWithETag: Endpoint[Unit, Option[Header.IfNoneMatch], ZNothing, Either[Header.ETag, (Catalogue, Header.ETag)], None] = Endpoint(RoutePattern.GET / "catalogueWithETag")
    .inCodec(HeaderCodec.ifNoneMatch.optional)
    .outCodec(StatusCodec.Ok ++ HttpCodec.content[Catalogue] ++ HttpCodec.header(Header.ETag))
    .outCodec(StatusCodec.NotModified ++ HttpCodec.header(Header.ETag))

  val reset: Endpoint[Unit, Unit, ZNothing, Unit, None] = Endpoint(RoutePattern.POST / "reset")
    .out[Unit].outCodec[Unit](HttpCodec.empty)

}
