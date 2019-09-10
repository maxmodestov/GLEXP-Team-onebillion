package org.onebillion.onecourse.mainui.oc_audiorec;

import org.onebillion.onecourse.controls.OBGroup;
import org.onebillion.onecourse.mainui.OBSectionController;
import org.onebillion.onecourse.utils.OBUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by michal on 07/06/2018.
 */

public class OC_AudioRecordingButton
{
    public static String  BUTTON_STATE_NORMAL = "normal", BUTTON_STATE_ACTIVE = "active",
            BUTTON_STATE_INACTIVE = "inactive",BUTTON_STATE_HIGHLIGHT = "highlight";

    public OBGroup control;
    private boolean animate;
    OBSectionController sectionController;


    public OC_AudioRecordingButton(OBGroup group,OBSectionController controller)
    {
        sectionController = controller;
        animate = false;
        control = group;
    }

    public void highlight()
    {
        control.objectDict.get(String.format("state_%s",BUTTON_STATE_HIGHLIGHT)).show();
    }

    public void setState(String state)
    {
        sectionController.lockScreen();
        control.setOpacity(1);
        control.hideMembers("dot_.*");
        control.hideMembers("state_.*");
        control.objectDict.get(String.format("state_%s",state)).show();
        sectionController.unlockScreen();
    }

    public void startRecordingAnimation() throws Exception
    {
        animate = true;
        OBUtils.runOnOtherThread(new OBUtils.RunLambda()
        {
            public void run() throws Exception
            {
                List<String> frames = Arrays.asList("dot_2","dot_1","dot_2","dot_3");
                int index = 0;
                while(animate && !sectionController.getAborting()) {
                    sectionController.lockScreen();
                    control.hideMembers("dot_.*");
                    control.objectDict.get(frames.get(index)).show();

                    sectionController.unlockScreen();
                    index++;
                    if (index >= frames.size())
                        index = 0;
                    sectionController.waitForSecs(0.3);
                }
            }

        });

    }

    public void stopRecordingAnimation()
    {
        animate = false;
    }

    public void flash(final long time, final float waitTime)
    {
        OBUtils.runOnOtherThread(new OBUtils.RunLambda()
        {
            public void run() throws Exception {
                if (time == sectionController.statusTime() && !sectionController.getAborting())
                    sectionController.waitForSecs(waitTime);
                if (time == sectionController.statusTime() && !sectionController.getAborting()) {
                    control.setOpacity(1);
                    sectionController.waitForSecs(0.5);

                }
                for (int i = 0;
                     i < 2;
                     i++) {
                    if (time == sectionController.statusTime() && !sectionController.getAborting()) {
                        control.setOpacity(0.4f);
                        sectionController.waitForSecs(0.3);

                    }
                    if (time == sectionController.statusTime() && !sectionController.getAborting()) {
                        control.setOpacity(1);
                        sectionController.waitForSecs(0.3);
                    }

                }
                control.setOpacity(1);
                if (time == sectionController.statusTime() && !sectionController.getAborting())
                    flash(sectionController.statusTime(), 5);
            }

        } );

    }


}
