package com.server.security;

import javax.servlet.ServletRequest;

public class PortResolver {
    //~ Instance fields ================================================================================================

    private PortMapper portMapper = new PortMapper();

    //~ Methods ========================================================================================================

    public PortMapper getPortMapper() {
        return portMapper;
    }

    public int getServerPort(ServletRequest request) {
        int serverPort = request.getServerPort();
        Integer portLookup = null;

        String scheme = request.getScheme().toLowerCase();

        if ("http".equals(scheme)) {
            portLookup = portMapper.lookupHttpPort(Integer.valueOf(serverPort));

        } else if ("https".equals(scheme)) {
            portLookup = portMapper.lookupHttpsPort(Integer.valueOf(serverPort));
        }

        if (portLookup != null) {
            // IE 6 bug
            serverPort = portLookup.intValue();
        }

        return serverPort;
    }

    public void setPortMapper(PortMapper portMapper) {
        this.portMapper = portMapper;
    }


}
