package team.tr.permitlog;

import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
//Small class to track the progress through the tutorial
public class TutorialListener implements IShowcaseListener {
    private int times = 0;

    public TutorialListener(){}

    public void onShowcaseDisplayed(MaterialShowcaseView showcaseView){
    }
    public void onShowcaseDismissed(MaterialShowcaseView showcaseView){
        times++;
    }
    public int getTutorialAmount(){
        return times;
    }
}
