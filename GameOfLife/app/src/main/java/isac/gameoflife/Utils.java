package isac.gameoflife;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Francesco on 14/03/2017.
 */

public class Utils {

    private static String ipAddress=null;
    private static Context context=null;

    private Utils(){
    }

    public static String getAddress(){
        File docsFolder = new File(Environment.getExternalStorageDirectory() + "/GameOfLife");

        if(!docsFolder.exists()) {
            docsFolder.mkdir();
        }

        File file=new File(docsFolder.getAbsolutePath(),"address.txt");

        if(!file.exists()){

            try {
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file/*, Context.MODE_PRIVATE*/);
                outputStream.write(("192.168.1.105").getBytes());
                outputStream.flush();
                outputStream.close();

                return "192.168.1.105";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String ret = "";

        try {
            FileInputStream inputStream=new FileInputStream(file);

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = bufferedReader.readLine();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(receiveString);
            inputStream.close();
            ret=stringBuilder.toString();
        }catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static void setContext(Context ctx){
        context=ctx;
    }

    public static String getIpAddress() {
        if (ipAddress == null) {

            if(context!=null) {
                WifiManager wm = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);
                try {
                    byte[] tmp = BigInteger.valueOf(wm.getConnectionInfo().getIpAddress()).toByteArray();
                    byte[] byteIpAddress = new byte[tmp.length];

                    for (int i = tmp.length - 1; i >= 0; i--) {
                        byteIpAddress[tmp.length - i - 1] = tmp[i];
                    }

                    ipAddress = InetAddress.getByAddress(byteIpAddress).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }else{
                return "127.0.0.1";
            }

        }
        System.out.println(ipAddress);
        return ipAddress;
    }
}
