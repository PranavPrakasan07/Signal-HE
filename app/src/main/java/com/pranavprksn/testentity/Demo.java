/*
 * DemoSignal — Demonstrate the signal protocol.
 * Copyright (C) 2017 Vijay Lakshminarayanan <lvijay@gmail.com>.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.pranavprksn.testentity;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;

public class Demo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        try {
            main();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main() throws Exception {
        /*
         * Create instances of the two parties.
         */
        Entity alice = new Entity(1, 314159, "alice");
        Entity bob = new Entity(2, 271828, "bob");


        /*
         * Establish a session between the two parties.
         */
        Session aliceToBobSession = new Session(alice.getStore(), bob.getPreKey(), bob.getAddress());

        /*
         * alice can now send messages to bob.
         */
        List<PreKeySignalMessage> toBobMessages = Arrays.stream("31,41,59,26,53".split(","))
                .map(msg -> {
                    try {
                        return aliceToBobSession.encrypt(msg);
                    } catch (UntrustedIdentityException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .collect(Collectors.toList());

        /*
         * For bob to read them, bob must know alice.
         */
        Session bobToAliceSession = new Session(bob.getStore(), alice.getPreKey(), alice.getAddress());

        /*
         * Now bob can decrypt them.
         */
        String fromAliceMessages = toBobMessages.stream()
                .map(encryptedMsg -> bobToAliceSession.decrypt(encryptedMsg))
                .peek(msg -> System.out.printf("Received from alice: '%s'%n", msg))
                .collect(joining(","));

        if (!fromAliceMessages.equals("31,41,59,26,53")) {
            throw new IllegalStateException("No match");
        }

        /*
         * bob, too, can send messages to alice.
         */
        List<PreKeySignalMessage> toAliceMessages = Arrays.stream("the quick brown fox".split(" "))
                .map(msg -> {
                    try {
                        return bobToAliceSession.encrypt(msg);
                    } catch (UntrustedIdentityException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .collect(toList());

        /*
         * And alice can read bob's messages.
         * Even if they arrive out of order.
         */
        Collections.shuffle(toAliceMessages);
        List<String> fromBobMessages = toAliceMessages.stream()
                .map(encryptedMsg -> aliceToBobSession.decrypt(encryptedMsg))
                .peek(msg -> System.out.printf("Received from bob: '%s'%n", msg))
                .collect(Collectors.toList());

        if (!(fromBobMessages.size() == 4
                && fromBobMessages.contains("the")
                && fromBobMessages.contains("quick")
                && fromBobMessages.contains("brown")
                && fromBobMessages.contains("fox"))) {
            throw new IllegalStateException("No match");
        }
    }
}
