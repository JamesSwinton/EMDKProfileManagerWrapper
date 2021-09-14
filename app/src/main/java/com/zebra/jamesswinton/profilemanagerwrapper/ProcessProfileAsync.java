package com.zebra.jamesswinton.profilemanagerwrapper;

import android.os.AsyncTask;
import android.util.Xml;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.ProfileManager.PROFILE_FLAG;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ProcessProfileAsync extends AsyncTask<String, Void, EMDKResults> {

  // Non-Static Variables
  private String mProfileName;
  private ProfileManager mProfileManager;
  private OnProfileApplied mOnProfileApplied;

  public ProcessProfileAsync(String profileName, ProfileManager profileManager,
      OnProfileApplied onProfileApplied) {
    this.mProfileName = profileName;
    this.mProfileManager = profileManager;
    this.mOnProfileApplied = onProfileApplied;
  }

  @Override
  protected EMDKResults doInBackground(String... params) {
    // Execute Profile
    return mProfileManager.processProfile(mProfileName, PROFILE_FLAG.SET, params);
  }

  @Override
  protected void onPostExecute(EMDKResults results) {
    super.onPostExecute(results);
    // Parse Results
    List<XmlParsingError> errors = new ArrayList<>();
    try {
      XmlPullParser xmlPullParser = Xml.newPullParser();
      xmlPullParser.setInput(new StringReader(results.getStatusString()));
      errors = parseXML(xmlPullParser);
    } catch (XmlPullParserException | IOException e) {
      e.printStackTrace();
      mOnProfileApplied.profileError(new XmlParsingError("Unknown", "Could not parse XML result - please check manually"));
      return;
    }

    // Notify Result
    if (errors.isEmpty()) {
      mOnProfileApplied.profileApplied();
    } else {
      mOnProfileApplied.profileError(errors.toArray(new XmlParsingError[0]));
    }
  }

  public interface OnProfileApplied {
    void profileApplied();
    void profileError(XmlParsingError... parsingErrors);
  }

  public List<XmlParsingError> parseXML(XmlPullParser myParser) throws IOException, XmlPullParserException {
    List<XmlParsingError> errors = new ArrayList<>();
    int event = myParser.getEventType();
    while (event != XmlPullParser.END_DOCUMENT) {
      String name = myParser.getName();
      switch (event) {
        case XmlPullParser.START_TAG:
          // Get Status, error name and description in case of
          // parm-error
          if (name.equals("parm-error")) {
            String errorName = myParser.getAttributeValue(null, "name");
            String errorDescription = myParser.getAttributeValue(null, "desc");
            errors.add(new XmlParsingError(errorName, errorDescription));

            // Get Status, error type and description in case of
            // parm-error
          } else if (name.equals("characteristic-error")) {
            String errorType = myParser.getAttributeValue(null, "type");
            String errorDescription = myParser.getAttributeValue(null, "desc");
            errors.add(new XmlParsingError(errorType, errorDescription));
          }
          break;
      } event = myParser.next();
    }
    return errors;
  }
}
