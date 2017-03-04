package dataengine.workers;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dataengine.api.Job;
import dataengine.api.Operation;
import dataengine.api.OperationParam;
import dataengine.api.OperationParam.ValuetypeEnum;
import dataengine.apis.OperationConsts;
import dataengine.apis.RpcClientProvider;
import dataengine.apis.SessionsDB_I;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class IngestPeopleWorker extends BaseWorker<Job> {

  @Inject
  public IngestPeopleWorker(RpcClientProvider<SessionsDB_I> sessDb) {
    super(OperationConsts.TYPE_INGESTER, sessDb);
  }

  @Override
  protected Operation initOperation() {
    Map<String, String> info = new HashMap<>();
    info.put(OperationConsts.OPERATION_TYPE, OperationConsts.TYPE_INGESTER);
    return new Operation()
        .id(jobType())
        .description("add source dataset 2")
        .info(info)
        .addParamsItem(new OperationParam()
            .key("inputUri").required(true)
            .description("location of source dataset")
            .valuetype(ValuetypeEnum.STRING).isMultivalued(false)
            .defaultValue(null))
        .addParamsItem(new OperationParam()
            .key(OperationConsts.DATA_FORMAT).required(false)
            .description("type and format of data 2")
            .valuetype(ValuetypeEnum.ENUM).isMultivalued(true)
            .defaultValue(null)
            .addPossibleValuesItem("PEOPLE.CSV"));
  }

}
