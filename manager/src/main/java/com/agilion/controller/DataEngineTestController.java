package com.agilion.controller;

import com.agilion.services.dataengine.DataEngineClient;
import com.agilion.services.dataengine.DataOperationReceipt;
import com.agilion.services.dataengine.interfaceV2.DataEngineInterface;
import com.agilion.services.dataengine.interfaceV2.DataOperationStatus;
import com.agilion.services.security.LoggedInUserGetter;
import com.agilion.services.security.NoLoggedInUserException;
import com.google.gson.Gson;
import dataengine.api.OperationSelection;
import dataengine.api.OperationSelectionMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Created by Alex_Lappy_486 on 1/19/18.
 */
@Controller
@RequestMapping("/admin/dataengine")
public class DataEngineTestController
{
    @Autowired
    DataEngineClient client;

    @Autowired
    Gson gson;

    @Autowired
    DataEngineInterface v2Client;

    @Autowired
    LoggedInUserGetter userGetter;

    @RequestMapping("/listOperations")
    public @ResponseBody
    String  listOperations()
    {
        return client.listOperations().toString();
    }

    @RequestMapping("/test")
    public String dataEngineTest(Model model)
    {
        model.addAttribute("operations", gson.toJson(client.listOperations()));
        return "admin/dataEngineTest";
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public @ResponseBody String dataEngineSubmit(@RequestBody Map<String, Object> data) throws Exception {
        DataOperationReceipt receipt = sendDataAsDataEngineRequest(data);
        return receipt.getSessionID();
    }

    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public @ResponseBody DataOperationStatus dataEngineStatus(@RequestBody String sessID) throws Exception {
        return this.v2Client.getDataEngineOperationStatus(new DataOperationReceipt(sessID, "NOTNEEDED"));
    }

    public DataOperationReceipt sendDataAsDataEngineRequest(Map<String, Object> data) throws NoLoggedInUserException
    {
        // Start with defining the basic properties of the operation
        OperationSelection operation = new OperationSelection();
        Map<String, Object> parameterMap = (Map<String, Object>) data.get("params");
        operation.setId(data.get("id").toString());
        operation.setParams(parameterMap);

        // Gather all sub-operations recursively
        Map<String, Object> topLevelSubOps = (Map<String, Object>) data.get("subOperations");
        buildSubOperations(operation, topLevelSubOps);

        return this.v2Client.submitDataEngineOperation(operation, userGetter.getCurrentlyLoggedInUser().getUsername());
    }

    private void buildSubOperations(OperationSelection parentOperation, Map<String, Object> subOpData)
    {
        // Build the SubOperation data structures and link them to the main operation
        OperationSelectionMap parentSubOperationSelections = new OperationSelectionMap();

        for (Map.Entry<String, Object> topLevelSubOp : subOpData.entrySet())
        {
            String subOpId = topLevelSubOp.getKey();
            Map<String, Object> topLevelSubOpMap = (Map<String, Object>) topLevelSubOp.getValue();
            Map<String, Object> topLevelSubOpParams = (Map<String, Object>) topLevelSubOpMap.get("params");
            OperationSelection subop = new OperationSelection().id(subOpId).params(topLevelSubOpParams);
            parentSubOperationSelections.put(subop.getId(), subop);

            // Recursively add this sub-op's sub-operations (ow my 'ead).
            if (topLevelSubOpMap.get("subOperations") != null && topLevelSubOpMap.get("subOperations") instanceof Map)
            {
                Map<String, Object> childSubopData = (Map<String, Object>) topLevelSubOpMap.get("subOperations");
                buildSubOperations(subop, childSubopData);
            }
        }

        parentOperation.setSubOperationSelections(parentSubOperationSelections);
    }
}