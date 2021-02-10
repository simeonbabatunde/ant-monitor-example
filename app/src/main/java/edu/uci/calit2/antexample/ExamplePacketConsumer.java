/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *
 *  AntMonitor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  AntMonitor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntMonitor. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.uci.calit2.antexample;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Patterns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.AhoCorasickInterface;
import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.PacketDumpInfo;

import static android.content.ContentValues.TAG;
import static edu.uci.calit2.antexample.PrivacyDB.TABLE_LEAKS_LOGS;
import static edu.uci.calit2.antmonitor.lib.util.IpDatagram.readDestinationIP;

/**
 * @author Anastasia Shuba
 */
public class ExamplePacketConsumer extends PacketConsumer {
    private PrivacyDB DATABASE;
    String[] address = {""};

    public ExamplePacketConsumer(Context context, TrafficType trafficType, String userID) {
        super(context, trafficType, userID);

        DATABASE = PrivacyDB.getInstance(context);

        String[] searchStrings = new String[] { "867241031467865", "867241031479985" };
        AhoCorasickInterface.getInstance().init(searchStrings);
    }
    @Override
    protected void consumePacket(PacketDumpInfo packetDumpInfo)
    {

        ConnectionValue cv = mapPacketToApp(packetDumpInfo);
        String AppName =  cv.getAppName();
        byte [] packet = packetDumpInfo.getDump();
        long TimeStamp = packetDumpInfo.getTimestamp();
//        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
//        calendar.setTimeInMillis(TimeStamp);
        //String date = DateFormat.format("dd-MM-yyyy hh:mm:ss",calendar).toString();
//        int  clength = packetDumpInfo.getCaptureLength();
//        int Olength = packetDumpInfo.getOriginalLength();
        String ip = readDestinationIP(packet);
        String hostname =  getIPHostname(ip);

        Log.d(TAG, " App Name:  " + AppName + "  destination : " + ip + "  DName "+ hostname );

//        Log.d(TAG, "packet  " +packet );
        ByteBuffer buffer = ByteBuffer.allocateDirect(packet.length);
        buffer.put(packet);

        // Search the current packet for the string
        final ArrayList<String> foundStrings = AhoCorasickInterface.getInstance().search(buffer, buffer.limit()-1);

        if (!(foundStrings == null && foundStrings.isEmpty()))
        {
            Log.d(TAG, " App Name:  " + AppName + " is leaking IMEI ");
            //DATABASE.logLeak(AppName,ip,"IMEI", TimeStamp);
            //DATABASE.close();

        }
//        Cursor c = DATABASE.getPrivacyLeaksAppHistory(AppName);
//
//
//        if(c.getCount() >0) {
//
//
//            String[] leaks = new String[c.getCount()];
//            Log.d(TAG, Arrays.toString(c.getColumnNames()));
//
//            int i = 0;
//            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
//
//                String toPrint = "";
//                for (int j = 0; j < c.getColumnCount(); j++)
//                    toPrint += c.getString(j) + ", ";
//                     Log.d(TAG, toPrint);
//
//                leaks[i] = toPrint;
//                i++;
//            }
//        }
//        c.close();
        DATABASE.getDatabase().delete(TABLE_LEAKS_LOGS, null, null);
       // mapIPtoHostName(packet);
        //log(packetDumpInfo, cv.getAppName());
    }

//    @Override
//    protected void consumePacket(PacketDumpInfo packetDumpInfo) {
//        ConnectionValue cv = mapPacketToApp(packetDumpInfo);
//        log(packetDumpInfo, cv.getAppName());
//    }

    /**
     * Parses the provided DNS data packet and fills out provided map with mappings of IP
     * address to host names. E.g., 216.58.193.196 -> www.google.com
     */
    public static void mapIPtoHostName(byte[] packet) {
        // See RFC 1035 for a reference on how this parsing works:
        // https://tools.ietf.org/html/rfc1035#section-4.1.3
        Log.d("NS", "inside mapIPHostName");
        //Log.d("NS", "flags " + packet[2] + " " + packet[3]);

        // TODO: check flags for a correct standard response

        int numQuestions = ((packet[4] << 8 & 0x0000FF00) | (packet[5] & 0x000000FF));
        Log.d("NS", "qs: " + numQuestions);

        int numAnswers = ((packet[6] << 8 & 0x0000FF00) | (packet[7] & 0x000000FF));
         Log.d("NS", "an: " + numAnswers);

        if (numQuestions != 1 || numAnswers == 0)
            return;
        //Log.d("NS","hit point");
        // 8, 9, 10, 11 are additional info we skip
        // TODO: assumng 1 question for now
        StringBuilder name = new StringBuilder();
        int i = 12;
        while (packet[i] != 0 && i < packet.length)
        {
            int labelLen = packet[i] & 0x000000FF;
            int position = i;
            i++;
            while (i <= position + labelLen) {
                name.append((char) (packet[i] & 0xFF));
                i++;
            }
            name.append(".");
        }
        Log.d("NS","name -> " + name);
        // Get rid of trailing "."
        String finalName = name.length() != 0 ? name.substring(0, name.length() - 1) : null;

        // Skip name and type, +1 for being AT start of answer
        i += 5;

        // Now parse the answers:
        for (int answer = 0; answer < numAnswers; answer++) {
            byte mask = (byte) 0b11000000; // 192 as an unsigned int, -64 as signed int
            int and = ((int) mask) & ((int) packet[i]);
            if (and != -64) {
                return; //TODO: deal with non-compressed names
            }

            // Skip the offset of compressed name
            i += 2;

            // get Answer type
            int answerTypeCode = ((packet[i++] << 8 & 0x0000FF00) | (packet[i++] & 0x000000FF));

            // Skip answer CLASS (2) and TTL (4)
            i += 6;

            // get the rdata length
            int answerRDataLength = ((packet[i++] << 8 & 0x0000FF00) | (packet[i++] & 0x000000FF));

            if (answerTypeCode != 1 || answerRDataLength != 4) {
                /* Skip past answers that:
                 *      - are not of type is A (value 1, host address) Section-3.2.2 of RFC 1035
                 *      - do not have data of length 4 since we only deal with IPv4 for now
                 */

                // Skip past this answer's RDATA for the next iteration:
                i += answerRDataLength;
                continue;
            }

            // read in IP!!
            String ipAddr = IpDatagram.ipv4addressBytesToString(packet[i++], packet[i++],
                    packet[i++], packet[i++]);
            Log.d("NS", "ip: " + ipAddr);
            Log.d("NS"," -> " + finalName);

        }
    }

    /*
    Function:   This methods accepts an IP address in dotted decimal notation and returns the
                equivalent host name. i.e. it maps IP address to Hostname. It uses a background
                thread to make network calls.
    Arg:        String representation of IP address in dotted decimal. e.g. "17.253.144.10"
    Returns:    String representing the canonical host name of the IP address. e.g. "icloud.com.cn"
*/
    String getIPHostname(final String dotted_IP_address){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress addr = InetAddress.getByName(dotted_IP_address);
                    address[0] = addr.getCanonicalHostName();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        return address[0];
    }
}
