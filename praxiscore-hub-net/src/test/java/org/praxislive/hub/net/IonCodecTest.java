/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2023 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.hub.net;

import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ControlPort;
import org.praxislive.core.Info;
import org.praxislive.core.Value;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PBytes;
import org.praxislive.core.types.PError;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PNumber;
import org.praxislive.core.types.PResource;
import org.praxislive.core.types.PString;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class IonCodecTest {

    private static final boolean VERBOSE = Boolean.getBoolean("praxis.test.verbose");

    private static final PBytes bytes = PBytes.valueOf(new byte[]{
        (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
    });

    @BeforeEach
    public void beforeEach(TestInfo info) {
        if (VERBOSE) {
            System.out.println("START TEST : " + info.getDisplayName());
        }
    }

    @AfterEach
    public void afterEach(TestInfo info) {
        if (VERBOSE) {
            System.out.println("END TEST : " + info.getDisplayName());
            System.out.println("=====================================");
        }
    }

    @Test
    public void testSendMessage() throws Exception {
        var matchID = 1234;
        var to = ControlAddress.of("/root/component.control");
        var from = ControlAddress.of("/sender/component.control");
        var args = List.of(
                Value.ofObject(10),
                Value.ofObject(1.234),
                PMap.of("key1", bytes, "key2", PArray.of(PNumber.ONE, PBoolean.FALSE))
        );
        var msg = new Message.Send(matchID, to, from, args);
        var msgList = roundTrip(List.of(msg));
        assertEquals(1, msgList.size());
        var decoded = (Message.Send) msgList.get(0);
        assertEquals(msg, decoded);
    }

    @Test
    public void testServiceMessage() throws Exception {
        var matchID = 1234;
        var service = "LogService";
        var control = "log";
        var from = ControlAddress.of("/sender/component.control");
        var args = List.of(
                Value.ofObject(10),
                Value.ofObject(1.234),
                PMap.of("key1", bytes, "key2", PArray.of(PNumber.ONE, PBoolean.FALSE))
        );
        var msg = new Message.Service(matchID, service, control, from, args);
        var msgList = roundTrip(List.of(msg));
        assertEquals(1, msgList.size());
        var decoded = (Message.Service) msgList.get(0);
        assertEquals(msg, decoded);
    }

    @Test
    public void testReplyMessage() throws Exception {
        var matchID = -987654321;
        var uri = URI.create("pkg:maven/org.praxislive/praxiscore-api@6.0");
        var msg = new Message.Reply(matchID, List.of(PResource.of(uri)));
        var msgList = roundTrip(List.of(msg));
        assertEquals(1, msgList.size());
        var decoded = (Message.Reply) msgList.get(0);
        assertEquals(matchID, decoded.matchID());
        assertEquals(uri, PResource.from(decoded.args().get(0)).orElseThrow().value());
    }

    @Test
    public void testErrorMessage() throws Exception {
        var matchID = 37707;
        Exception ex;
        try {
            throw new IllegalStateException("FOO");
        } catch (Exception e) {
            ex = e;
        }
        var error = PError.of(ex);
        var msg = new Message.Error(matchID, List.of(error));
        var msgList = roundTrip(List.of(msg));
        assertEquals(1, msgList.size());
        var decoded = (Message.Error) msgList.get(0);
        assertEquals(matchID, decoded.matchID());
        var decodedArg = decoded.args().get(0);
        assertTrue(decodedArg instanceof PError);
        var decodedError = PError.from(decodedArg).orElseThrow();
        assertEquals(IllegalStateException.class.getSimpleName(), decodedError.errorType());
        assertEquals("FOO", decodedError.message());
    }

    @Test
    public void testSystemMessage() throws Exception {
        var matchID = 1;
        var type = "HELLO";
        var data = PMap.of(Utils.KEY_REMOTE_SERVICES,
                PMap.of("LogService", "org.praxislive.hub.net.FooLogService"),
                Utils.KEY_FILE_SERVER_PORT, 13178);
        var msg = new Message.System(matchID, type, data);
        var msgList = roundTrip(List.of(msg));
        assertEquals(1, msgList.size());
        var decoded = (Message.System) msgList.get(0);
        assertEquals(msg, decoded);
    }

    @Test
    public void testMultiMessage() throws Exception {
        var msg1 = new Message.Send(1,
                ControlAddress.of("/root1.trigger"),
                ControlAddress.of("/root2.process"),
                List.of(PString.of("FOO"), PNumber.ZERO),
                PMap.EMPTY
        );
        var msg2 = new Message.Service(2,
                "FooService",
                "process",
                ControlAddress.of("/root2.process"),
                List.of(bytes),
                PMap.EMPTY
        );
        var msg3 = new Message.Reply(3, List.of(PString.of("OK")), PMap.EMPTY);
        var msg4 = new Message.Error(4, List.of(), PMap.EMPTY);
        var msg5 = new Message.System(5, "STATUS", PMap.of("active", true));
        var msgList = roundTrip(List.of(msg1, msg2, msg3, msg4, msg5));
        assertEquals(5, msgList.size());
        assertEquals(msg1, msgList.get(0));
        assertEquals(msg2, msgList.get(1));
        assertEquals(msg3, msgList.get(2));
        assertEquals(msg4, msgList.get(3));
        assertEquals(msg5, msgList.get(4));

    }

    @Test
    public void testMapBasedValues() throws Exception {
        int matchID = -987654321;
        var customProp = PMap.of("flag", true, "data", bytes);
        var info = Info.component()
                .merge(ComponentProtocol.API_INFO)
                .port("in", Info.port().input(ControlPort.class).build())
                .port("out", Info.port().output(ControlPort.class).build())
                .property("custom", customProp)
                .build();
        var msg = new Message.Reply(matchID, List.of(info));
        var msgList = roundTrip(List.of(msg));
        assertEquals(1, msgList.size());
        var decoded = (Message.Reply) msgList.get(0);
        assertEquals(matchID, decoded.matchID());
        var decodedInfo = decoded.args().get(0);
        assertTrue(info.equivalent(decodedInfo));
        assertTrue(decodedInfo instanceof ComponentInfo);
        assertEquals(info, decodedInfo);
        var byteProp = ComponentInfo.from(decodedInfo)
                .flatMap(i -> PMap.from(i.properties().get("custom")))
                .map(m -> m.get("data"))
                .orElseThrow();
        assertEquals(bytes, byteProp);
    }

    private List<Message> roundTrip(List<Message> messages) throws Exception {
        byte[] data = IonCodec.getDefault().writeMessages(messages);
        if (VERBOSE) {
            var sb = new StringBuilder();
            sb.append("\nMESSAGE\n=======\n");
            var system = IonSystemBuilder.standard().build();
            try (IonWriter writer = IonTextWriterBuilder.pretty().build(sb)) {
                writer.writeValues(system.newReader(data));
            }
            sb.append("\n\nBinary Size : ").append(data.length).append("\n\n");
            System.out.println(sb);
        }
        return IonCodec.getDefault().readMessages(data);
    }

}
