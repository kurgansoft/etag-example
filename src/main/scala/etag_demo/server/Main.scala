package etag_demo.server

import etag_demo.common.{Catalogue, EndpointDefinitions}
import zio.http.{Route, Routes, Server}
import zio.{&, Duration, Fiber, Ref, Scope, ZEnvironment, ZIO, ZIOAppDefault}

object Main extends ZIOAppDefault {

  override def run: ZIO[Scope, Throwable, Unit] = for {
    catalogueRef <- Ref.make(CatalogueGenerator.data(1))
    updateFiberId <- updateCatalogueEffect.provideEnvironment(ZEnvironment(catalogueRef)).fork
    fiberIdRef <- Ref.make(updateFiberId)
    routes = Routes(getCatalogueRoute, resetRoute).provideEnvironment(ZEnvironment(catalogueRef).add(fiberIdRef))
    _ <- Server.serve(routes).provide(Server.default)
    _ <- ZIO.never
  } yield ()

  private[server] val getCatalogueRoute: Route[Ref[Catalogue], Nothing] = EndpointDefinitions.getCatalogue.implement(_ =>
    for {
      ref <- ZIO.service[Ref[Catalogue]]
      currentCatalogue <- ref.get
    } yield currentCatalogue
  )

  private val resetRoute: Route[Ref[Fiber.Runtime[Nothing, Unit]] & Ref[Catalogue], Nothing] = EndpointDefinitions.reset.implement(_ => for {
    _ <- zio.Console.printLine("\t\t\t... reset ...").orDie
    fiberIdRef <- ZIO.service[Ref[zio.Fiber.Runtime[Nothing, Unit]]]
    catalogueRef <- ZIO.service[Ref[Catalogue]]
    _ <- fiberIdRef.get.flatMap(_.interrupt)
    newFiberId <- updateCatalogueEffect.provideEnvironment(ZEnvironment(catalogueRef)).forkDaemon
    _ <- fiberIdRef.set(newFiberId)
  } yield ())

  private val updateCatalogueEffect: ZIO[Ref[Catalogue], Nothing, Unit] = for {
    catalogueRef <- ZIO.service[Ref[Catalogue]]
    _ <- ZIO.foreachDiscard(1 to CatalogueGenerator.size) {
      round =>
        for {
          _ <- catalogueRef.set(CatalogueGenerator.data(round))
          _ <- ZIO.sleep(Duration.fromMillis(500))
        } yield ()
    }
  } yield ()
}
