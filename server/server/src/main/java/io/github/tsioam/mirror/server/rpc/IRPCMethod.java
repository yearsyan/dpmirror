package io.github.tsioam.mirror.server.rpc;

import java.io.IOException;

import io.github.tsioam.mirror.server.device.ConfigurationException;

public interface IRPCMethod {
    String onRequest(IPCContext context) throws ConfigurationException, IOException;
}