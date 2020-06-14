/**
 *  BadHost  by  https://github.com/Burning-machine    2020 - 2021.
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

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import bad.host.util.Decompress;
import static bad.host.Intro.unzip;
public class MainActivity extends Activity {
    public static final int MULTIPLE_PERMISSIONS = 12;
    //Folder name containing host files on the root of the Sdcard/phone storage
    //private static final String FOLDERNAME = "BadHost/0.4.6a1";
    private static final String FOLDERNAME = "BadHost/latest";
    private WebServer server = null;
    private Thread thread = null;
    private TextView v1 = null;
    private TextView v2 = null;
    private String[] permissions;
    public int Http_port;
    public int Dns_port;
    //Supported formats
    public String[] formats = {".svg",".md", ".png", ".mp4", ".manifest", ".js", ".css", ".html", ".bin", ".nro", ".map",".jpg", ""};
    public Button start = null;
    public Button stop = null;
    public String path;
    public FileInputStream fis =null;
    public File f = null;
    public File root = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //HTTP port used to start the server
        Http_port = 8080;
        //DNS port used to start the server
        //even as root we can't start on port 53
        Dns_port = 3233;
        //required permissions
        permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAnimation();
        v1 = findViewById(R.id.text1);
        v2 = findViewById(R.id.text2);
        checkPermissions();
        rootexec();
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!(Utils.getIPAddress(true).equals(""))) {
                    if (unzip) {
                        unzipHost();
                        unzip=false;
                    }
                    if (server != null) {
                        if (server.isAlive())
                            server.stop();
                        start();
                        showNotification("Server Started", "Running on " + Utils.getIPAddress(true), MainActivity.this, false);
                    } else {
                        if (server == null) startfdns();
                        start();
                        showNotification("Server Started", "Running on " + Utils.getIPAddress(true), MainActivity.this, false);
                    }
                } else
                    Toast.makeText(MainActivity.this, "Turn on Hotspot/Connection", Integer.parseInt("4000")).show();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (server != null) {
                    if (server.isAlive())
                        server.stop();
                    v1.setText("HTTP IP : " + "\t\t\t" + " Stopped");
                    showNotification("Server Stopped", "HTTP Server Stopped ", MainActivity.this, false);
                    Log.d("HTTP", "stopped :)");
                }
            }
        });
        if (checkFirstRun()) startIntro();
    }


    public void start() {
        Log.d("HTTP", "Current ip address is : " + "http://" + Utils.getIPAddress(true));
        v1.setText("HTTP IP :" + "\t\t\t" + Utils.getIPAddress(true));
        Thread HTTPthread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                server = new WebServer();
                try {
                    server.start();
                } catch (IOException ioe) {
                    Log.w("MainActivity", "The server could not start.");
                }
            }
        });
        HTTPthread.start();
        Log.w("MainActivity", "Web server initialized.");
    }

    //Running DNS on it's own thread it uses the devices current ip when started
    public void startfdns() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                ServerKernelProgram serve = new ServerKernelProgram();
                String IP_Server = "manuals.playstation.net";
                try {
                    serve.receiveANDanswer(Dns_port, IP_Server, Utils.getIPAddress(true));
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        v2.setText(v2.getText() + "\t\t\t" + Utils.getIPAddress(true));
    }

    public String isSupported(String uri) {
        for (int i = 0; i < formats.length; i++) {
            if (uri.endsWith(formats[i])) return formats[i];
        }
        return null;
    }

    private class WebServer extends NanoHTTPD {
        public WebServer() {
            super(Http_port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            if (uri.equals("/") || (uri.contains("/document"))) uri = "/index.html";
            String format = isSupported(uri);
            root = Environment.getExternalStorageDirectory();
            path = root.getAbsolutePath() + "/" + FOLDERNAME + uri;
            try {
                f = new File(path);
                fis = new FileInputStream(f);
                if (format != null) {
                    switch (format) {
                        case (".html"):
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.OK, "text/html", fis);
                        case (".js"):
                            return newChunkedResponse(Response.Status.ACCEPTED,"text/javascript",fis);
                        case (".css"):
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.OK, "text/css", fis);
                        case (".manifest"):
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.OK, "text/cache-manifest", fis);
                        case (".png"):
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.OK, "image/png", fis);
                        case (".mp4"):
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.OK, "video/mp4", fis);
                        case (".jpg"):
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.OK, "image/jpg", fis);
                        default:
                            Log.d("HTTPee", uri + " requested");
                            return newChunkedResponse(Response.Status.ACCEPTED, "application/octet-stream", fis);
                    }
                }
                else {
                    Log.d("HTTPee", "Unsupported format");
                    return null;
                }
            }
            catch (FileNotFoundException e) {
                //e.printStackTrace();
                Log.d("HTTPee", uri + " File not found");
            }
            return null;
        }
    }

    //the only part that needs root access
        public void rootexec() {
            try {
                Runtime.getRuntime().exec("su");
                //Bypassing Android's < 1024 port restrictions
                Runtime.getRuntime().exec("su -c iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port " + Http_port);
                Runtime.getRuntime().exec("su -c iptables -t nat -A PREROUTING -p udp --dport " + Dns_port + " -j REDIRECT --to-port 53");
                Runtime.getRuntime().exec("su -c iptables -t nat -A PREROUTING -p tcp --dport " + Dns_port + " -j REDIRECT --to-port 53");
                Runtime.getRuntime().exec("su -c iptables -t nat -A PREROUTING -p udp --dport 53 -j REDIRECT --to-port " + Dns_port);
                Runtime.getRuntime().exec("su -c iptables -t nat -A PREROUTING -p tcp --dport 53 -j REDIRECT --to-port " + Dns_port);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("App", "Failed to get root");
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public static void showNotification(String title, String message, Context ctx, Boolean t) {
            NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("CUSTOM_CHANNEL",
                        "CHANNEL_UPDATE",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DESCRIPTION");
                mNotificationManager.createNotificationChannel(channel);
            }
            Intent broadcast = new Intent(ctx, Breceiver.class);
            PendingIntent actionIntent = PendingIntent.getBroadcast(ctx, 0, broadcast, PendingIntent.FLAG_ONE_SHOT);
            Notification.Builder mBuilder = new Notification.Builder(ctx, "CUSTOM_CHANNEL");
            mBuilder.setSmallIcon(R.drawable.ic_action_ps) // notification icon
                    .setContentTitle(title) // title for notification
                    .setContentText(message)// message for notification
                    .setContentIntent(actionIntent)
                    .addAction(R.drawable.ic_action_ps, "Close", actionIntent);
            //Closing app
            if (t) {
                mBuilder.setTimeoutAfter(2);
                mBuilder.setAutoCancel(true);
            }

            Notification noti = mBuilder.build();
            mNotificationManager.notify(0, noti);
        }

        public void unzipHost() {
            Thread threadunzip = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + FOLDERNAME + "/";
                    //Both zip files are in the assets folder
                    Decompress.unzipFromAssets(MainActivity.this, "Static Host.zip", path);
                    //Decompress.unzipFromAssets(MainActivity.this, "ps4-exploit-host-0.4.6a1-android.zip", path);
                }
            });
            threadunzip.start();

        }

        public boolean checkFirstRun() {
            boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
            return isFirstRun;
        }

        public void startIntro() {
            if (checkFirstRun()) {
                Intent intent = new Intent(this, Intro.class);
                startActivity(intent);
            }
        }

        public static void completedFirstRun(Context ctx) {
            ctx.getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();
        }

        private void setAnimation() {
            ConstraintLayout linearLayout = (ConstraintLayout) findViewById(R.id.layoutMain);
            AnimationDrawable animationDrawable = (AnimationDrawable) linearLayout.getBackground();
            animationDrawable.setEnterFadeDuration(2500);
            animationDrawable.setExitFadeDuration(5000);
            animationDrawable.start();
        }

        private void checkPermissions() {
            int result;
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String p : permissions) {
                result = ContextCompat.checkSelfPermission(MainActivity.this, p);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                }
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            }
        }
    }