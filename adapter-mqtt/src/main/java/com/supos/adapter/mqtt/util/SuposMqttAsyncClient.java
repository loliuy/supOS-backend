package com.supos.adapter.mqtt.util;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttOutputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class SuposMqttAsyncClient extends MqttAsyncClient {
    private MqttOutputStream out;
    private final AtomicLong nextId = new AtomicLong();
    private long hasData;

    public SuposMqttAsyncClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
    }

    public void flush() {
        synchronized (this) {
            if (out != null && hasData > 0) {
                try {
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    hasData = 0;
                }
            }
        }
    }

    public void publishAsync(String topics, MqttMessage message) throws MqttException {
        MqttPublish pubMsg = new MqttPublish(topics, message);
        pubMsg.setMessageId(nextId.intValue());
        MqttOutputStream outputStream = getOutputStream();
        synchronized (this) {
            try {
                outputStream.write(pubMsg);
                hasData++;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (hasData >= 100) {
                flush();
            }
        }
    }

    public void publishBatch(String[] topics, byte[][] messages) throws MqttException, IOException {
        MqttOutputStream outputStream = getOutputStream();
        for (int i = 0; i < topics.length; i++) {
            MqttPublish pubMsg = new MqttPublish(topics[i], new MqttMessage(messages[i]));
            pubMsg.setMessageId(nextId.intValue());
            outputStream.write(pubMsg);
        }
        outputStream.flush();
    }

    private MqttOutputStream getOutputStream() {
        MqttOutputStream outputStream;
        synchronized (this) {
            if (out == null) {
                try {
                    NetworkModule module = comms.getNetworkModules()[0];
                    out = new MqttOutputStream(comms.getClientState(), module.getOutputStream()) {
                        public void write(MqttWireMessage message) throws IOException, MqttException {
                            byte[] bytes = message.getHeader();
                            byte[] pl = message.getPayload();
                            out.write(bytes, 0, bytes.length);
                            int offset = 0;
                            int chunckSize = 1024;
                            while (offset < pl.length) {
                                int length = Math.min(chunckSize, pl.length - offset);
                                out.write(pl, offset, length);
                                offset += chunckSize;
                            }
                        }
                    };
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            outputStream = this.out;
        }
        return outputStream;
    }

    @Override
    public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException {
        IMqttToken token = super.disconnect(userContext, callback);
        synchronized (this) {
            out = null;
        }
        return token;
    }

    @Override
    public void reconnect() throws MqttException {
        super.reconnect();
        synchronized (this) {
            out = null;
        }
    }
}

