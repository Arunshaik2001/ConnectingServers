package com.example.connectservers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ArrayList<Integer> pingType=new ArrayList<>();
    ArrayList<String> connName=new ArrayList<>();
    ArrayList<String> urls=new ArrayList<>();
    ArrayList<String> responses=new ArrayList<>();

    TextView[][] pingTextView=new TextView[20][3];

    String[] colors=new String[]{"#5d9356","#ff0000"};

    Thread _clockThread;
    boolean _clockStopSignal;

    Thread _statusThread;
    boolean _statusStopSignal;

    String configFileTxt="";

    ImageView DataConnectionImageView;
    TextView ConnectionTypeTextView,AvailableStatusTypeTextView,wifiBroadCastRecieverExtView,ConnectionTextView;
    BroadcastReceiver wifiBroadCastReciever=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SupplicantState supplicantState;
            WifiManager wifiManager=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            supplicantState=wifiManager.getConnectionInfo().getSupplicantState();

            if(supplicantState.equals(SupplicantState.COMPLETED)){
                Toast.makeText(MainActivity.this,"connected",Toast.LENGTH_SHORT).show();
                DataConnectionImageView.setImageResource(R.drawable.presence_online);
                wifiBroadCastRecieverExtView.setText("Connected");
                wifiBroadCastRecieverExtView.setTextColor(Color.parseColor(colors[0]));
                ConnectionTypeTextView.setText("WIFI");
                ConnectionTypeTextView.setTextColor(Color.parseColor(colors[0]));
                ConnectionTextView.setText("CONNECTED");
                ConnectionTextView.setTextColor(Color.parseColor(colors[0]));  // red

                AvailableStatusTypeTextView.setText("AVAILABLE");
                AvailableStatusTypeTextView.setTextColor(Color.parseColor(colors[0]));  // red
            }
            else if(supplicantState.equals(SupplicantState.SCANNING)){
                Toast.makeText(MainActivity.this,"scanning",Toast.LENGTH_SHORT).show();
                DataConnectionImageView.setImageResource(R.drawable.presence_busy);
                wifiBroadCastRecieverExtView.setText("Scanning");
                wifiBroadCastRecieverExtView.setTextColor(Color.parseColor(colors[1]));
                ConnectionTypeTextView.setText("UNKNOWN");
                ConnectionTypeTextView.setTextColor(Color.parseColor(colors[1]));
                ConnectionTextView.setText("NOT CONNECTED");
                ConnectionTextView.setTextColor(Color.parseColor(colors[1]));  // red

                AvailableStatusTypeTextView.setText("UNAVAILABLE");
                AvailableStatusTypeTextView.setTextColor(Color.parseColor(colors[1]));  // red
            }
            else{
                Toast.makeText(MainActivity.this,"no wifi",Toast.LENGTH_SHORT).show();
                DataConnectionImageView.setImageResource(R.drawable.presence_invisible);
                wifiBroadCastRecieverExtView.setText("no wifi");
                wifiBroadCastRecieverExtView.setTextColor(Color.parseColor(colors[1]));
                ConnectionTypeTextView.setText("UNKNOWN");
                ConnectionTypeTextView.setTextColor(Color.parseColor(colors[1]));

                ConnectionTextView.setText("NOT CONNECTED");
                ConnectionTextView.setTextColor(Color.parseColor(colors[1]));  // red

                AvailableStatusTypeTextView.setText("UNAVAILABLE");
                AvailableStatusTypeTextView.setTextColor(Color.parseColor(colors[1]));  // red
            }

        }
    };

    @Override
    protected void onStart() {
        DataConnectionImageView=findViewById(R.id.image1);
        ConnectionTypeTextView=findViewById(R.id.textType);
        AvailableStatusTypeTextView=findViewById(R.id.textAvail);
        ConnectionTextView=findViewById(R.id.textConn);
        wifiBroadCastRecieverExtView=findViewById(R.id.textBroadcastReceiver);
        IntentFilter intentFilter=new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(wifiBroadCastReciever,intentFilter);

        getNetworkInfo();

        super.onStart();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(wifiBroadCastReciever);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pingType.clear();
        connName.clear();
        urls.clear();
        responses.clear();
        try {

            Resources resources=getResources();
            InputStream inputStream=resources.openRawResource(R.raw.configfile);
            byte[] b=new byte[inputStream.available()];
            inputStream.read(b);

            configFileTxt=new String(b);
            JSONArray jsonConfigArray=new JSONArray(configFileTxt);
            for(int i=0;i<jsonConfigArray.length();i++){
                JSONArray serverObjectArray=jsonConfigArray.getJSONArray(i);
                Integer type=(Integer) jsonGetter(serverObjectArray,"type");
                String conName=(String) jsonGetter(serverObjectArray,"name");
                String url=(String) jsonGetter(serverObjectArray,"url");
                String response=(String) jsonGetter(serverObjectArray,"res");

                pingType.add(type);
                connName.add(conName);
                urls.add(url);
                responses.add(response);

            }

        }catch (Exception e){
            Toast.makeText(this, "Reading Config File Error", Toast.LENGTH_SHORT).show();
        }
        _statusUpdate();
        _clockRun();
    }


    public boolean getNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        ConnectionTypeTextView.setText("UNKNOWN");
        if(networkInfo!=null) {
            if (networkInfo.getTypeName().equalsIgnoreCase("WIFI")) {
                ConnectionTypeTextView.setText("WIFI");
                ConnectionTypeTextView.setTextColor(Color.parseColor(colors[0]));
            } if(networkInfo.getTypeName().equalsIgnoreCase("MOBILE")) {
                ConnectionTypeTextView.setText("MOBILE");
                ConnectionTypeTextView.setTextColor(Color.parseColor(colors[1]));
            }
            if (networkInfo.isAvailable()) {
                AvailableStatusTypeTextView.setText("Available");
                AvailableStatusTypeTextView.setTextColor(Color.parseColor(colors[0]));
                if (networkInfo.isConnected()) {
                    ConnectionTextView.setText("Connected");
                    ConnectionTextView.setTextColor(Color.parseColor(colors[0]));
                    return true;
                } else {
                    ConnectionTextView.setText("Not Connected");
                    ConnectionTextView.setTextColor(Color.parseColor(colors[1]));
                    return false;
                }

            } else {
                AvailableStatusTypeTextView.setText("UnAvailable");
                AvailableStatusTypeTextView.setTextColor(Color.parseColor(colors[1]));
                return false;
            }


        }
        else {
            ConnectionTypeTextView.setText("UNKNOWN");

            ConnectionTextView.setText("NOT CONNECTED");
            ConnectionTextView.setTextColor(Color.parseColor(colors[1]));  // red

            AvailableStatusTypeTextView.setText("UNAVAILABLE");
            AvailableStatusTypeTextView.setTextColor(Color.parseColor(colors[1]));  // red

        }
        return false;
    }

    public String getTime(){
        Date date=new Date();
        Integer seconds=date.getSeconds();
        String inSeconds=String.format("%02d",seconds);
        Integer mins=date.getMinutes();
        String inMinutes=String.format("%02d",mins);
        Integer hrs=date.getHours();
        String inHrs=String.format("%02d",hrs);

        return inHrs+" : "+inMinutes+" : "+inSeconds;
    }

    public  void _clockRun(){
        _clockStopSignal=false;
        _clockThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (!_clockStopSignal){
                    try {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateClock();
                            }
                        });
                        Thread.sleep(1000);
                    }catch (Exception e){
                        _clockStopSignal=true;
                        Toast.makeText(MainActivity.this, "Time error", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
        _clockThread.start();
    }

    public void updateClock(){
        TextView timeContinuousTextView=findViewById(R.id.timeContinuos);
        timeContinuousTextView.setText(getTime());
    }

    public Object jsonGetter(JSONArray jsonArray,String key){
        try {
            for (int i=0;i<jsonArray.length();i++){
                JSONObject jsonObject=jsonArray.getJSONObject(i);
                if(jsonObject.has(key)){
                    return jsonObject.get(key);
                }
            }
        }catch (Exception e){
             Toast.makeText(MainActivity.this,"Error while getting json Object",Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public void _statusUpdate(){
        _statusStopSignal=false;
        _statusThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (!_statusStopSignal){
                    try {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus();
                            }
                        });
                        Thread.sleep(20000);
                    }
                    catch (Exception e){
                        _statusStopSignal=true;
                        Toast.makeText(MainActivity.this, "Cannot Update Status", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        _statusThread.start();
    }

    public void updateStatus(){
        if(getNetworkInfo()){
            DataConnectionImageView.setImageResource(R.drawable.presence_online);
        }else{
            DataConnectionImageView.setImageResource(R.drawable.presence_busy);
        }

        LinearLayout pingLinearLayout=findViewById(R.id.insertPings);
        pingLinearLayout.removeAllViews();

        LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5,5,5,5);
        layoutParams.gravity= Gravity.CENTER;


        for(int i=0;i<connName.size();i++){
            LinearLayout newLL=new LinearLayout(this);
            newLL.setLayoutParams(layoutParams);
            newLL.setOrientation(LinearLayout.HORIZONTAL);
            newLL.setHorizontalGravity(Gravity.START);
            newLL.setPadding(5,5,5,5);
            pingLinearLayout.addView(newLL,i);

            pingTextView[i][0] = new TextView(MainActivity.this);
            pingTextView[i][0].setText(connName.get(i));
            pingTextView[i][0].setTextSize(10);
            pingTextView[i][0].setPadding(5,5,5,5);
            pingTextView[i][0].setTextColor(Color.parseColor("#000000"));

            newLL.addView(pingTextView[i][0], 0 );

            pingTextView[i][1] = new TextView(MainActivity.this);
            pingTextView[i][1].setText(urls.get(i));
            pingTextView[i][1].setTextSize(10);
            pingTextView[i][1].setPadding(5,5,5,5);

            newLL.addView(pingTextView[i][1], 1);

            pingTextView[i][2] = new TextView(MainActivity.this);
            pingTextView[i][2].setText(responses.get(i));
            pingTextView[i][2].setTextSize(10);
            pingTextView[i][2].setPadding(5,5,5,5);
            newLL.addView(pingTextView[i][2], 2);

            if(pingType.get(i)==0){
                new pingICMP(urls.get(i),i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else{
                new pingHTTP(urls.get(i),i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        }


        TextView refreshTime=findViewById(R.id.refreshedTime);
        refreshTime.setText(getTime());
    }

    private class pingHTTP extends AsyncTask<Void, String, Integer> {
        private String urlString;
        private boolean ping_success;
        private int item;
        private int status;

        private pingHTTP(String ip, int i) {
            ping_success = false;
            item = i;
            urlString = ip;
        }
        protected Integer doInBackground(Void ...params) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setAllowUserInteraction(false);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setRequestMethod("GET");
                httpConn.connect();
                status = httpConn.getResponseCode();
                // Check for successful status code = 200 or 204
                if ((status == HttpURLConnection.HTTP_OK) || (status == HttpURLConnection.HTTP_NO_CONTENT)) ping_success = true;
            } catch (Exception e) {
                // Handle exception
                ping_success = false;
            }
            return 1;
        }
        protected void onProgressUpdate(String msg) { }
        protected void onPostExecute(Integer result) {
            if (ping_success) {
                pingTextView[item][2].setText("Status Code= " + status);
                pingTextView[item][2].setTextColor(Color.parseColor(colors[0]));  // green
            } else {
                pingTextView[item][2].setText("Status Code= " + status);
                pingTextView[item][2].setTextColor(Color.parseColor(colors[1]));  // red
            }
        }
    }

    public class pingICMP extends AsyncTask<Void, String, Integer> {
        private String ip1;
        private boolean code;
        private int item;
        private InetAddress in1;

        public pingICMP(String ip, int i) {
            ip1 = ip;
            in1 = null;
            item = i;
            code = false;
        }

        protected Integer doInBackground(Void ...params) {
            try {
                in1 = InetAddress.getByName(ip1);
            } catch (Exception e) {
                code = false;
            }
            try {
                if(in1.isReachable(5000)) {
                    code = true;
                } else {
                    code = false;
                }
            } catch (Exception e) {
                code = false;
            }
            return 1;
        }

        protected void onPostExecute(Integer result) {
            if (code) {
                pingTextView[item][2].setText("Reachable");
                pingTextView[item][2].setTextColor(Color.parseColor(colors[0]));  // green
            } else {
                pingTextView[item][2].setText("Not Reachable");
                pingTextView[item][2].setTextColor(Color.parseColor(colors[1]));  // red
            }
        }
    }
}