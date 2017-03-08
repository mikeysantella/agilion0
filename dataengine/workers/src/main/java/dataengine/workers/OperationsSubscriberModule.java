package dataengine.workers;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import dataengine.apis.VerticleConsts;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
final class OperationsSubscriberModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(Vertx.class);
  }

  static int subscriberCounter = 0;

  public static void deployOperationsSubscriberVerticle(Injector injector, Worker_I worker) {
    Vertx vertx = injector.getInstance(Vertx.class);
    OperationsSubscriberVerticle opsRegVert =
        new OperationsSubscriberVerticle(VerticleConsts.opsRegBroadcastAddr,
            "ops" + (++subscriberCounter) + "-" + worker.name() + System.currentTimeMillis(),
            worker);
    log.info("VERTX: WORKER: Deploying OperationsSubscriberVerticle: {} for {}", opsRegVert, worker); 
    vertx.deployVerticle(opsRegVert);
  }
}
