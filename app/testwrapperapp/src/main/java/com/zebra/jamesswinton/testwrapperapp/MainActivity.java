package com.zebra.jamesswinton.testwrapperapp;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements OnXmlProcessedListener {

  // Profile Manager
  private EMDKProfileManagerWrapper mEmdkProfileManagerWrapper;

  // Constants
  public static final String DEFAULT_KEYBOARD_PROFILE_NAME = "WrapperLibProfile";
  public static final String DEFAULT_KEYBOARD_PROFILE_XML =
            "<wap-provisioningdoc>\n"
          + "  <characteristic version=\"10.1\" type=\"UiMgr\">\n"
          + "    <parm name=\"InputMethodAction\" value=\"1\" />\n"
          + "    <characteristic type=\"InputMethodDetails\">\n"
          + "      <parm name=\"InputMethodOption\" value=\"4\" />\n"
          + "      <parm name=\"InputMethodPackageName\" value=\"com.symbol.mxmf.csp.enterprisekeyboard\" />\n"
          + "      <parm name=\"InputMethodClassName\" value=\"com.android.inputmethod.latin.LatinIME\" />\n"
          + "    </characteristic>\n"
          + "  </characteristic>\n"
          + "</wap-provisioningdoc>";

  public static final String NULL_KEYBOARD_PROFILE_NAME = "NullKeyboard";
  public static final String NULL_KEYBOARD_PROFILE_XML =
            "<wap-provisioningdoc>\n"
          + "  <characteristic type=\"Profile\">\n"
          + "    <parm name=\"ProfileName\" value=\"NullKeyboard\"/>\n"
          + "    <characteristic version=\"10.1\" type=\"UiMgr\">\n"
          + "      <parm name=\"InputMethodAction\" value=\"1\" />\n"
          + "      <characteristic type=\"InputMethodDetails\">\n"
          + "        <parm name=\"InputMethodOption\" value=\"4\" />\n"
          + "        <parm name=\"InputMethodPackageName\" value=\"com.wparam.nullkeyboard\" />\n"
          + "        <parm name=\"InputMethodClassName\" value=\"com.wparam.nullkeyboard.NullKeyboard\" />\n"
          + "      </characteristic>\n"
          + "    </characteristic>\n"
          +  "  </characteristic>\n"
          +  "</wap-provisioningdoc>";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mEmdkProfileManagerWrapper = new EMDKProfileManagerWrapper(this, this);
    mEmdkProfileManagerWrapper.applyXml(DEFAULT_KEYBOARD_PROFILE_XML);
  }

  @Override
  public void onComplete() {
    Log.i("", "complete");
  }

  @Override
  public void onError(String... errors) {
    for (String error : errors) {
      Log.i("XML Errors: ", error);
    }
  }
}