package etag_demo.client

import etag_demo.common.Catalogue
import etag_demo.common.EndpointDefinitions.{getCatalogue, reset}
import zio.http.Client
import zio.http.endpoint.EndpointExecutor
import zio.{Ref, Scope, ZIO, ZIOAppDefault}

object Client1 extends ZIOAppDefault with EndpointLocatorResolver {

  override def run: ZIO[Scope, Unit, Unit] = (for {
    endpointLocator <- resolveEndpointLocator
    client <- ZIO.service[Client]
    executor = EndpointExecutor(client, endpointLocator)

    _ <- ZIO.log("calling reset endpoint")
    _ <- executor(reset())

    catalogueRef <- Ref.make[Option[Catalogue]](None)
    noOfBandwidthWastingCalls <- Ref.make[Int](0)

    _ <- ZIO.foreachDiscard(1 to 250)(_ => for {
      latestCatalogue <- executor(getCatalogue())
      _ <- ZIO.log("The latest catalogue retrieved: " + latestCatalogue)
      currentCatalogue <- catalogueRef.get
      _ <- if (currentCatalogue.exists(_.version == latestCatalogue.version))
        noOfBandwidthWastingCalls.update(_ + 1) *> ZIO.log(s"Seems like we have fetched catalogue with version [${latestCatalogue.version}], but we had that one already.")
      else
        catalogueRef.set(Some(latestCatalogue))
      _ <- ZIO.sleep(zio.Duration.fromMillis(50))
    } yield ())

    noOfBandwidthWastingCallsAsNumber <- noOfBandwidthWastingCalls.get
    _ <- ZIO.log(s"\n\tWe have executed $noOfBandwidthWastingCallsAsNumber bandwidth wasting calls.")
  } yield ()).provideSome(
    Client.default.orDie
  )
}
