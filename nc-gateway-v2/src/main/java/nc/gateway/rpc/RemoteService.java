package nc.gateway.rpc;

import nc.gateway.annotation.RPCMethod;
import nc.gateway.annotation.RPCService;

import java.util.Map;

@RPCService(scfname = "remoteServer")
public interface RemoteService {

    @RPCMethod(cmd="testCmd", needLogin=false)
    public String testMethod(Map<String, String> parms);
}
