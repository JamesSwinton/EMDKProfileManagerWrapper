package com.zebra.jamesswinton.profilemanagerwrapper;

import android.app.Activity;
import android.os.AsyncTask;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.zebra.jamesswinton.profilemanagerwrapper.ProcessProfileAsync.OnProfileApplied;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EMDKProfileManagerWrapper implements EMDKManager.EMDKListener, OnProfileApplied {

  // EMDK EMDKManager
  private EMDKManager mEmdkManager;
  private ProfileManager mProfileManager;
  private boolean mObtainingEmdkManager = false;

  // Callback & Context
  private Activity mActivity;
  private ProfileManagerWrapperCallback mCallback;

  public EMDKProfileManagerWrapper(Activity activity, ProfileManagerWrapperCallback callback) {
    // Set Callback
    this.mActivity = activity;
    this.mCallback = callback;

    // Grab EMDK
    if (mEmdkManager == null) {
      mObtainingEmdkManager = obtainEmdk();
    }
  }

  private boolean obtainEmdk() {
    // Clear Old Instance
    if (mEmdkManager != null) {
      mEmdkManager.release();
      mEmdkManager = null;
    }

    // Init EMDK
    EMDKResults emdkManagerResults = EMDKManager.getEMDKManager(mActivity, this);

    // Verify EMDK Manager
    if (emdkManagerResults == null || emdkManagerResults.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
      mCallback.onError("Could not obtain EMDKManager");
      return false;
    }

    // EMDK Being Obtained...
    return true;
  }

  public void applyXml(String xml) {
    // Append Profile Name to Xml
    xml = wrapXmlInProfile(xml);

    // Validate EMDK
    if (mEmdkManager == null && !mObtainingEmdkManager) {
      mCallback.onError("Could not obtain EMDKManager, "
          + "please re-instantiate this class & try again. This error is usually caused by "
          + "another application holding onto the EMDKManager");
    }

    // Validate ProfileManager
    if (mProfileManager == null && !mObtainingEmdkManager) {
      mCallback.onError("Could not obtain ProfileManager, "
          + "please re-instantiate this class & try again.");
    }

    // Queue request if EMDK not ready
    if ((mEmdkManager == null || mProfileManager == null) && mObtainingEmdkManager) {
      mCallback.onError("EMDK Not ready - please move your code to the onReady() callback");
      return;
    }

    // Process Profile
    new ProcessProfileAsync("WrapperLibProfile", mProfileManager, this)
        .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, xml);
  }

  private String wrapXmlInProfile(String xml) {
    xml = xml.replace("<wap-provisioningdoc>", "");
    xml = xml.replace("</wap-provisioningdoc>", "");
    return "<wap-provisioningdoc>\n" + "  <characteristic type=\"Profile\">\n"
        + "    <parm name=\"ProfileName\" value=\"WrapperLibProfile\"/>\n"
        + xml + "  </characteristic>\n" + "</wap-provisioningdoc>";
  }

  public void release() {
    if (mEmdkManager != null) {
      mEmdkManager.release();
      mEmdkManager = null;
    }

    if (mProfileManager != null) {
      mProfileManager = null;
    }
  }

  /******************
   * EMDK Callbacks *
   ******************/

  @Override
  public void onOpened(EMDKManager emdkManager) {
    // Clear Flag
    mObtainingEmdkManager = false;

    // Assign EMDK Reference
    mEmdkManager = emdkManager;

    // Get Profile & Version Manager Instances
    mProfileManager = (ProfileManager) mEmdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

    // Check ProfileManager
    if (mProfileManager == null) {
      mCallback.onError("Could not obtain Profile Manager");
    }
  }

  @Override
  public void onClosed() {
    // Clear Flag
    mObtainingEmdkManager = false;

    // Release EMDK Manager Instance
    if (mEmdkManager != null) {
      mEmdkManager.release();
      mEmdkManager = null;
    }
  }

  /***************************
   * Xml Processed Callbacks *
   ***************************/

  @Override
  public void profileApplied() {
    mCallback.onXmlProcessed();
  }

  @Override
  public void profileError(String... errors) {
    mCallback.onXmlError(errors);
  }

  /************
   * Callback *
   ************/

  public interface OnXmlProcessedListener {
    void onComplete();
    void onError(String... errors);
  }

  public interface ProfileManagerWrapperCallback {
    void onReady();
    void onError(String error);
    void onXmlProcessed();
    void onXmlError(String... errors);
  }

}
