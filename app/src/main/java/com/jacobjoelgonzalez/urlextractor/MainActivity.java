package com.jacobjoelgonzalez.urlextractor;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;


public class MainActivity extends Activity {

    //Layout views
    private EditText urlTextField;
    private TextView displayTextField;

    //Constants for communication between the UI thread and the url extraction thread
    public static final int MESSAGE_RETURN_VALUE = 1;
    public static final String RETURN_VALUE = "return_value";

    //Handler for incoming messages from the urlThread
    Handler urlHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize view variables
        urlTextField = (EditText) findViewById(R.id.urlTextField);
        urlTextField.setImeActionLabel(getString(R.string.extract), 1);
        urlTextField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == 1) {
                    //Get url and display data
                    displayURL(v);
                    handled = true;
                }
                return handled;
            }
        });

        displayTextField = (TextView) findViewById(R.id.displayTextField);
        displayTextField.setMovementMethod(new ScrollingMovementMethod());
        displayTextField.setHorizontallyScrolling(true);

        //Initialize handler
        urlHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_RETURN_VALUE){
                    String message = msg.getData().getString(RETURN_VALUE);
                    displayTextField.setText(message);
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Called when extract button is clicked
    public void displayURL(View view) {
        //Hide soft keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlTextField.getWindowToken(), 0);

        //Get the url string
        String url = urlTextField.getText().toString();

        //Extract and display data returned by url
        UrlThread urlThread = new UrlThread(url, urlHandler);
        urlThread.start();
    }

    /**
     * This thread is to handle communication
     */
    private class UrlThread extends Thread{
        //URL input
        private String urlStr;

        //Validated URL
        private String urlString;

        //Handler for communication with the UI thread
        Handler handler = null;

        //Take in user input and UI handler
        public UrlThread(String url, Handler handler){
            urlStr = url;
            this.handler = handler;
        }

        /**
         * Sends a return value for the displayTextField in the UI thread.
         * @param returnVal - the return value to be sent.
         */
        private void returnValue(String returnVal){
            Message msg = handler.obtainMessage(MESSAGE_RETURN_VALUE);
            Bundle bundle = new Bundle();
            bundle.putString(RETURN_VALUE, returnVal);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }

        @Override
        public void run() {

            try {
                //Check for valid url length
                if(urlStr.length() < 4){
                    throw new MalformedURLException();
                }
                if(urlStr.substring(0,4).equalsIgnoreCase("http")){
                    urlString = urlStr;
                }
                else{
                    urlString = "http://" + urlStr;
                }
                URL url = new URL(urlString);

                //Open an input stream to get data from url
                InputStream in = url.openStream();

                //Read source with java.util.scanner
                Scanner scan = new Scanner(in);
                StringBuilder lines = new StringBuilder(0);
                while(scan.hasNextLine()){
                    String line = scan.nextLine();
                    lines.append(line+"\n");
                }
                String doc = lines.toString();

                //Close resources
                in.close();
                scan.close();

                //Display source code
                returnValue(doc);

            } catch (MalformedURLException e) {
                returnValue("Sorry! That URL is invalid.");
            } catch (IOException e) {
                returnValue("Looks like this website doesn't exist :-/ \n\nTry prefixing the URL with 'HTTPS://' to fix the problem.");
            }
        }
    }

}