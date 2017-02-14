package team.tr.permitlog;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class AboutFragment extends Fragment {
    //For logging:
    public static String TAG = "AboutFragment";
    //The root view for this fragment, used to find elements by id:
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Get the WebView element:
        LayoutInflater lf = getActivity().getLayoutInflater();
        rootView =  lf.inflate(R.layout.fragment_about, container, false);
        WebView webView = (WebView) rootView.findViewById(R.id.webView);
        //Load the HTML file in the WebView:
        webView.loadUrl("file:///android_asset/about.html");
        return rootView;
    }
}
