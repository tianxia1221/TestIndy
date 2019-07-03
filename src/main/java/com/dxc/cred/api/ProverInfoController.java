package com.dxc.cred.api;

import com.dxc.cred.domain.ProverInfo;
import com.dxc.cred.domain.Result;
import com.dxc.cred.service.ProverInfoServiceImpl;

import org.hyperledger.indy.sdk.IndyException;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@EnableAutoConfiguration
@RequestMapping(value = "/api", produces = "application/json")
public class ProverInfoController {
    @Autowired
    private ProverInfoServiceImpl proverInfoService;

    static Logger logger = LoggerFactory.getLogger(ProverInfoController.class);

    @RequestMapping(value = "/provers" , method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public void createProverInfos(@RequestBody List<ProverInfo> proverInfos, HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        proverInfoService.createProverInfoList(proverInfos);
    }

    @RequestMapping(value = "/active-provers", method = RequestMethod.GET)
    public List<ProverInfo> getActiveProverInfoList(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        List<ProverInfo> proverInfos = proverInfoService.getActiveInfoList();
        return proverInfos;
    }
    
    @RequestMapping(value = "/expired-provers", method = RequestMethod.GET)
    public List<ProverInfo> getExpiredProverInfoList(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        List<ProverInfo> proverInfos = proverInfoService.getExpiredInfoList();
        return proverInfos;
    }
    
    @RequestMapping(value = "/history-provers", method = RequestMethod.GET)
    public List<ProverInfo> getHistoryProverInfoList(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        List<ProverInfo> proverInfos = proverInfoService.getHistoryInfoList();
        return proverInfos;
    }

    @RequestMapping(value = "/provers" , method = RequestMethod.PUT)
    @ResponseBody
    public Result submitProverInfos(@RequestBody List<ProverInfo> proverInfos, HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        proverInfoService.updateNonceByUuidList(proverInfos);
        return new Result("success", "", "");
    }

    @RequestMapping(value = "/provers/{pid}" , method = RequestMethod.DELETE)
    @ResponseBody
    public Result deleteProverInfo(@PathVariable String pid, HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        proverInfoService.deleteProverInfoByUuid(pid);
        return new Result("delete", "", "");
    }
}
