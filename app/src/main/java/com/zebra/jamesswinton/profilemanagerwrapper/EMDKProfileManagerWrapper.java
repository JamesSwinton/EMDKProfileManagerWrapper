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
  // Xml
  private List<String> mProfiles = new ArrayList<>();

  // EMDK EMDKManager
  private EMDKManager mEmdkManager;
  private ProfileManager mProfileManager;
  private boolean mObtainingEmdkManager = false;

  // Callback & Context
  private Activity mActivity;
  private OnXmlProcessedListener mOnXmlProcessedListener;

  // Main Thread Handler
  private final ScheduledExecutorService mScheduledWorker = Executors.newSingleThreadScheduledExecutor();

  public EMDKProfileManagerWrapper(Activity activity, OnXmlProcessedListener onXmlProcessedListener) {
    // Set Callback
    this.mActivity = activity;
    this.mOnXmlProcessedListener = onXmlProcessedListener;

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
      mOnXmlProcessedListener.onError("Could not obtain EMDKManager");
      return false;
    }

    // EMDK Being Obtained...
    return true;
  }

  public String mXml = "";
  public void applyXml(String xml) {
    // Append Profile Name to Xml
    mXml = wrapXmlInProfile(xml);

    // Add Xml To List
    mProfiles.add(mXml);

    // Validate EMDK
    if (mEmdkManager == null && !mObtainingEmdkManager) {
      mOnXmlProcessedListener.onError("Could not obtain EMDKManager, "
          + "please re-instantiate this class & try again. This error is usually caused by "
          + "another application holding onto the EMDKManager");
    }

    // Validate ProfileManager
    if (mProfileManager == null && !mObtainingEmdkManager) {
      mOnXmlProcessedListener.onError("Could not obtain ProfileManager, "
          + "please re-instantiate this class & try again.");
    }

    // Queue request if EMDK not ready
    if ((mEmdkManager == null || mProfileManager == null) && mObtainingEmdkManager) {
      mScheduledWorker.schedule(() -> applyXml(mXml), 500, TimeUnit.MILLISECONDS);
      return;
    }

    // Process Profile
    for (String profile : mProfiles) {
      new ProcessProfileAsync("WrapperLibProfile", mProfileManager, this)
          .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, profile);
    }

    // Clear Queue
    mProfiles.clear();
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
      mOnXmlProcessedListener.onError("Could not obtain Profile Manager");
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
    mOnXmlProcessedListener.onComplete();
  }

  @Override
  public void profileError(String... errors) {
    mOnXmlProcessedListener.onError(errors);
  }

  /************
   * Callback *
   ************/

  public interface OnXmlProcessedListener {
    void onComplete();
    void onError(String... errors);
  }

}
