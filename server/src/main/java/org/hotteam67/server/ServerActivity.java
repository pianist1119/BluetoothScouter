package org.hotteam67.server;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.*;
import android.view.*;
import android.os.Message;
import java.io.*;
import java.util.*;

import android.os.Environment;

import org.hotteam67.common.BluetoothActivity;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;


public class ServerActivity extends BluetoothActivity
{
    public static final String TEAM_NUMBER_SCHEMA =
            "Team 14,Team 24,Team 34,Team 44,Team 54,Team 64";

    public static final int MATCH_NUMBER = 1;

    FileWriter databaseFile = null;
    String content = "";

    TextView connectedDevicesText;
    TextView teamsReceivedText;
    TextView latestMatchText;

    Button sendConfigurationButton;
    Button sendTeamsButton;

    NumberPicker match;

    TableLayout teamsLayout;

    List<List<String>> matches = new ArrayList<>();
    CheckBox autoSendTeams;

    android.support.v7.widget.Toolbar toolbar;

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);


        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setCustomView(R.layout.toolbar_server);
        ab.setDisplayShowCustomEnabled(true);
        // setRequestedOrientation(getResources().getConfiguration().orientation);

        l("Setting up io");
        setupIO();

        l("Setting up ui");
        connectedDevicesText = (TextView)findViewById(R.id.connectedDevices);
        teamsReceivedText = (TextView)findViewById(R.id.teamsReceived);
        latestMatchText = (TextView)findViewById(R.id.latestMatch);

        sendConfigurationButton = (Button)findViewById(R.id.saveButton);
        sendConfigurationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                l("Sending configuration");
                String s = loadSchema();
                if (!s.trim().isEmpty())
                    Write(s);
                else
                    l("No configuration found");
            }
        });
        sendTeamsButton = (Button) findViewById(R.id.sendTeamsButton);
        sendTeamsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                l("Sending Teams");
                SendTeams();
            }
        });

        autoSendTeams = (CheckBox) findViewById(R.id.autoSendBox);

        match = (NumberPicker) findViewById(R.id.matchNumber);
        match.setMinValue(1);
        match.setMaxValue(200);
        match.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                LoadTeams();
            }
        });

        teamsLayout = (TableLayout) findViewById(R.id.teamNumberLayout);
        for (TableRow row : SchemaHandler.GetRows(TEAM_NUMBER_SCHEMA, this))
        {
            teamsLayout.addView(row);
        }


        /*
        outputView = (TableLayout)findViewById(R.id.outputView);


        for (int i = 0; i < 15; ++i)
        {
            content += "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z\n";
        }
        l("Content:\n" + content);
        List<String> data = new ArrayList<>(Arrays.asList(content.split("\n")));
        ServerOutputAdapter.Build(this, data, outputView);
        */

        LoadTeams();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try
        {
            if (databaseFile != null)
                databaseFile.close();
        }
        catch (IOException e)
        {
            l("Failed to close database file");
        }
    }

    int teamsReceived = 0;
    int matchNumber = 0;

    private void handleInput(String msg)
    {
        l("Handling input: Value: " + msg);

        try
        {
            if (databaseFile != null)
            {
                l("Writing to database file: " + msg);
                if (databaseFile != null)
                    databaseFile.append(msg + "\n");
                databaseFile.flush();
                content += msg;
            }
            else
            {
                l("Tried to write, database file was null!");
            }
        }
        catch (IOException e)
        {
            l("Failed to write to file on receive: " + e.getMessage());
            e.printStackTrace();
        }

        List<String> vars = new ArrayList<>(Arrays.asList(msg.split(",")));

        try
        {
            int match = Integer.valueOf(vars.get(MATCH_NUMBER));
            if (match != matchNumber)
                teamsReceived = 0;
            matchNumber = match;
        }
        catch (Exception e)
        {
            l("Invalid match #: " + vars.get(MATCH_NUMBER));
            e.printStackTrace();
        }

        teamsReceived++;
        teamsReceivedText.setText("Teams Received: " + teamsReceived);
        latestMatchText.setText("Latest Game #: " + matchNumber);

        if (teamsReceived >= 6 && autoSendTeams.isChecked())
        {
            SendTeams();
        }
    }

    private void LoadTeams()
    {
        try
                        {
            List<String> teams = matches.get(match.getValue() - 1);
            SchemaHandler.SetCurrentValues(teamsLayout, teams);
        }
        catch (Exception e)
        {
            l("Failed to load next match from arraylist.");
        }
    }

    private void SendTeams()
    {
        List<String> teams = SchemaHandler.GetCurrentValues(teamsLayout);
        l("Teams loaded: " + teams.size());
        String output = match.getValue() + ",";
        match.setValue(match.getValue() + 1);
        for (int i = 0; i < teams.size(); ++i)
        {
            if (i < connectedThreads.size())
            {
                l("Writing: " + output + teams.get(i));
                Write(output + teams.get(i), i);
            }
        }
        LoadTeams();
    }

    private int connectedDevices = 0;

    @Override
    protected synchronized void handle(Message msg)
    {
        switch (msg.what)
        {
            case MESSAGE_INPUT:
                String message = (String)msg.obj;
                handleInput(message);
                break;
            case MESSAGE_TOAST:
                l(new String((byte[])msg.obj));
                break;
            case MESSAGE_CONNECTED:
                connectedDevices++;
                connectedDevicesText.setText("Devices Connected: " + connectedDevices);
                // toast("CONNECTED!");
                break;
            case MESSAGE_DISCONNECTED:
                connectedDevices--;
                connectedDevicesText.setText("Devices Connected: " + connectedDevices);
                l(
                        "Disconnect. Connected Devices: "
                                + connectedDevices
                                + " Socket Count: "
                                + connectedThreads.size());
                // toast("Device Disconnected!");
                break;
        }
    }

    private void setupIO()
    {
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            l("No Access to SD Card!!");
        }

        loadExistingContent();

        loadWriter();
    }

    private String loadSchema()
    {
        String line = FileHandler.LoadContents(FileHandler.SCHEMA);
        return line.replace("\n", "");
    }

    private void loadExistingContent()
    {
        try
        {
            content = FileHandler.LoadContents(FileHandler.SERVER);
        }
        catch (Exception e)
        {
            l("Unable to read from file: " + e.getMessage());
        }

        try
        {
            BufferedReader reader = FileHandler.GetReader(FileHandler.MATCHES);
            String line = reader.readLine();
            while (line != null)
            {
                if (line.split(",").length==6)
                {
                    matches.add(Arrays.asList(line.split(",")));
                }
                line = reader.readLine();
            }
        }
        catch (Exception e)
        {
            l("Load teams failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadWriter()
    {
        databaseFile = FileHandler.GetWriter(FileHandler.SERVER);

        // Update header with schema
        String s = loadSchema();
        if (!s.trim().isEmpty())
        {
            try
            {
                l("Loading databasefile for header write");
                l("Writing new header line");
                List<String> oldString = new ArrayList<>(Arrays.asList(content.split("\n")));
                if (oldString.size() > 0)
                    oldString.remove(0);
                oldString.add(0, SchemaHandler.GetHeader(FileHandler.LoadContents(FileHandler.SCHEMA)));
                l("Old String size: " + oldString.size());
                l("Building string again");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < oldString.size(); ++i)
                    builder.append(oldString.get(i) + "\n");
                l("Writing: " + builder.toString());
                databaseFile.write(builder.toString());
                databaseFile.flush();
            }
            catch (IOException e)
            {
                l("Failed to open file for header writing. Something went wrong??");
                e.printStackTrace();
            }
        }
    }
}
