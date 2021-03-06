package dataengine.jobmgr;

import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph;
import dataengine.apis.CommunicationConsts;
import dataengine.apis.JobBoardInput_I;
import dataengine.apis.JobDTO;
import dataengine.apis.RpcClientProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.deelam.activemq.MQClient;
import net.deelam.activemq.rpc.ActiveMqRpcServer;
import net.deelam.activemq.rpc.AmqComponentSubscriber;
import net.deelam.activemq.rpc.KryoSerDe;
import net.deelam.graph.GrafUri;
import net.deelam.graph.IdGrafFactoryNeo4j;
import net.deelam.graph.IdGrafFactoryTinker;

@RequiredArgsConstructor
@Slf4j
public class JobBoardModule extends AbstractModule {
  final String jobBoardRpcAddr;
  final String newJobAvailableTopic;
  final Connection connection;
  
  @Override
  protected void configure() {
    requireBinding(Connection.class);
    
    Consumer<JobDTO> newJobPublisher=createNewJobPublisher(connection, newJobAvailableTopic);
    JobBoard jobBoard = new JobBoard(newJobPublisher);
    jobBoard.setJobBoardRpcAddr(jobBoardRpcAddr);
    bind(JobBoard.class).toInstance(jobBoard);
  }

  private Consumer<JobDTO> createNewJobPublisher(Connection connection, String topicName) {
    try {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer producer = MQClient.createTopicMsgSender(session, topicName, DeliveryMode.NON_PERSISTENT);
      KryoSerDe serde=new KryoSerDe(session); 
      return jobDto -> {
        try {
          Message message = serde.writeObject(jobDto);
          producer.send(message);
          log.info("Published new job notification: {}", jobDto.getId());
        } catch (JMSException e) {
          log.error("When notifying JobConsumers of new job={}", jobDto, e); 
        }
      };
    } catch (JMSException e) {
      throw new IllegalStateException("When creating NewJobPublisher for JobBoard", e); 
    }
  }

  static void deployJobBoard(Injector injector) throws JMSException {
    JobBoard jobBoard = injector.getInstance(JobBoard.class);
    if(DEBUG){
      jobBoard.periodicLogs(10_000, 20);
    }
    
    log.info("AMQ: TASKER: Deploying JobBoard: {} ", jobBoard); 
    injector.getInstance(ActiveMqRpcServer.class).start(jobBoard.getJobBoardRpcAddr(), jobBoard, true);
    
    Connection connection=injector.getInstance(Connection.class);
    new AmqComponentSubscriber(connection, "JobBoard", 
        CommunicationConsts.RPC_ADDR, jobBoard.getJobBoardRpcAddr(), 
        CommunicationConsts.COMPONENT_TYPE, "JobBoard");
  }

  static void deployDepJobService(Injector injector, String dispatcherRpcAddr) throws JMSException {
    GrafUri depJobGrafUri;
    
    if(false){
      IdGrafFactoryNeo4j.register();
      depJobGrafUri = new GrafUri("neo4j:jobMgrDB");
    } else {
      IdGrafFactoryTinker.register();
      depJobGrafUri = new GrafUri("tinker:/");
    }
    IdGraph<?> depJobMgrGraf = depJobGrafUri.openIdGraph();

    // requires that jobProducerProxy be deployed
    RpcClientProvider<JobBoardInput_I> jbInputRpc=
        injector.getInstance(Key.get(new TypeLiteral<RpcClientProvider<JobBoardInput_I>>() {}));
    DepJobService depJobMgr = new DepJobService(depJobMgrGraf, jbInputRpc);
    depJobMgr.setDispatcherRpcAddr(dispatcherRpcAddr);
    log.info("AMQ: TASKER: Deploying RPC service for DepJobService: {}", depJobMgr); 
    injector.getInstance(ActiveMqRpcServer.class).start(depJobMgr.getDispatcherRpcAddr(), depJobMgr, true);
    
    Connection connection=injector.getInstance(Connection.class);
    new AmqComponentSubscriber(connection, "Dispatcher", 
        CommunicationConsts.RPC_ADDR, dispatcherRpcAddr, 
        CommunicationConsts.COMPONENT_TYPE, "JobDispatcher");

//    if(DEBUG){
//      vertx.setPeriodic(10_000, t -> {
//        if (depJobMgr.getWaitingJobs().size() > 0)
//          log.info("waitingJobs={}", depJobMgr.getWaitingJobs().keySet());
//        if (depJobMgr.getUnsubmittedJobs().size() > 0)
//          log.info("unsubmittedJobs={}", depJobMgr.getUnsubmittedJobs().keySet());
//      });
//      
//      int statusPeriod = 10_000;
//      int sameLogThreshold = 10;
//      if (statusPeriod > 0) {
//        AtomicInteger sameLogMsgCount = new AtomicInteger(0);
//        vertx.setPeriodic(statusPeriod, id -> {
//          String logMsg = depJobMgr.toStringRemainingJobs(DepJobFrame.STATE_PROPKEY);
//          if (!logMsg.equals(depJobMgrPrevLogMsg)) {
//            log.info(logMsg);
//            depJobMgrPrevLogMsg = logMsg;
//            sameLogMsgCount.set(0);
//          } else {
//            if (sameLogMsgCount.incrementAndGet() > sameLogThreshold)
//              depJobMgrPrevLogMsg = null;
//          }
//        });
//      }
//    }
  }
  
  private static final boolean DEBUG = true;
  private static String depJobMgrPrevLogMsg;

}

