package org.onebillion.onecourse.mainui.oc_lettersandsounds;

import android.graphics.PointF;

import org.onebillion.onecourse.controls.OBControl;
import org.onebillion.onecourse.controls.OBGroup;
import org.onebillion.onecourse.controls.OBLabel;
import org.onebillion.onecourse.controls.OBPath;
import org.onebillion.onecourse.mainui.generic.OC_Generic;
import org.onebillion.onecourse.utils.OBAnim;
import org.onebillion.onecourse.utils.OBAnimationGroup;
import org.onebillion.onecourse.utils.OBAudioManager;
import org.onebillion.onecourse.utils.OBUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pedroloureiro on 01/07/16.
 */
public class OC_Alpha4 extends OC_Alpha3
{

    public OC_Alpha4 ()
    {
        super();
    }

    @Override
    public void prepare ()
    {
        super.prepare();
        events = new ArrayList<>(Arrays.asList("a", "b"));
    }

    @Override
    public void doAudio (String scene) throws Exception
    {
        List audio = currentAudio("PROMPT");
        List replayAudio = currentAudio("REPEAT");
        //
        setReplayAudio(replayAudio);
        playAudioQueued(audio, false);
        //
        if (flashBoxTimeStamp == 0) action_flashRow();

        OBUtils.runOnOtherThread(new OBUtils.RunLambda()
        {
            @Override
            public void run () throws Exception
            {
                doReminder();
            }
        });
    }

    @Override
    public void doMainXX () throws Exception
    {
        setStatus(STATUS_AWAITING_CLICK);
        doAudio(currentEvent());
    }

    @Override
    public void setSceneXX (String scene)
    {
        super.setSceneXX(scene);
        phase = 0;
        firstBox = 0;
        lastBox = letters.size();
    }

    @Override
    public void demoa () throws Exception
    {
        setStatus(STATUS_DOING_DEMO);
        //
        loadPointer(POINTER_MIDDLE);
        OC_Generic.pointer_moveToRelativePointOnScreen(0.9f, 1.3f, 0f, 0.1f, true, this);
        //  q
        OBControl box = boxes.get(lastBox - 1);
        PointF position = OC_Generic.copyPoint(box.position());
        position.y += 0.75 * box.height();
        //
        action_playNextDemoSentence(false); // Let's say the alphabet
        OC_Generic.pointer_moveToRelativePointOnScreen(0.94f, 0.8f, 0, 0.6f, true, this);
        waitAudio();
        waitForSecs(0.3);
        //
        if (currentAudio("DEMO").size() > 2)
        {
            action_playNextDemoSentence(true); // This time you'll see the capital letters.
            waitForSecs(0.3);
        }
        box = boxes.get(20);
        position = OC_Generic.copyPoint(box.position());
        position.x += 1.4 * box.width();
        position.y += 0.5 * box.height();
        //
        action_playNextDemoSentence(false); // Touch the grid.
        movePointerToPoint(position, -5, 0.6f, true);
        waitAudio();
        waitForSecs(0.3);
        //
        thePointer.hide();
        waitForSecs(0.3);
        setStatus(STATUS_AWAITING_CLICK);
        //
        doAudio(currentEvent());
        //
        action_flashRow();
    }

    @Override
    public void demob () throws Exception
    {
        doMainXX();
    }

    public void action_showLetters () throws Exception
    {
        if (!currentEvent().equals(events.get(0))) return;
        //
        List<OBAnim> animations = new ArrayList();
        //
        lockScreen();
        for (int i = 0; i < labels.size(); i++)
        {
            OBLabel label = labels.get(i);
            if (i >= firstBox && i < lastBox)
            {
                OBGroup back = backs.get(i);
                OBControl clone = back.copy();
                back.setScreenMaskControl(clone);
//                OBGroup group = new OBGroup(new ArrayList(Arrays.asList(back)));
//                attachControl(group);
//                group.setZPosition(back.zPosition() + 0.1f);
//                group.show();
//                group.setMasksToBounds(true);
                //
                PointF destination = OC_Generic.copyPoint(back.position());
                destination.x -= 1.1 * back.width();
                OBAnim animation = OBAnim.moveAnim(destination, back);
                animations.add(animation);
                label.setOpacity(1.0f);
            }
            else
            {
                label.setOpacity(0.3f);
            }
        }
        unlockScreen();
        //
        playSfxAudio("reveal", false);
        double duration = OBAudioManager.audioManager.durationSFX();
        OBAnimationGroup.runAnims(animations, duration, true, OBAnim.ANIM_EASE_IN_EASE_OUT, this);
        waitForSecs(0.3);
    }


    public void checkBox (OBPath box)
    {
        setStatus(STATUS_CHECKING);
        //
        try
        {
            Boolean correctBox = action_isCorrectBox(box);
            if (correctBox)
            {
                action_flashRow_stop();
                waitForSecs(0.6);
                //
                if (!currentEvent().equals(events.get(events.size() - 1)))
                {
                    action_showLetters();
                    playSceneAudio("CORRECT", true); // Get ready to join in!
                    waitForSecs(0.3);
                }
                //
                action_introLetters();
                waitForSecs(0.3);
                //
                if (currentEvent().equals(events.get(events.size() - 1)))
                {
                    waitForSecs(0.3);
                    //
                    playSceneAudio("FINAL", true);
                    waitForSecs(0.3);
                }
                else if (currentEvent().equals(events.get(events.size() - 2)))
                {
                    lockScreen();
                    for (OBLabel label : labels)
                    {
                        label.setOpacity(1.0f);
                    }
                    unlockScreen();
                    gotItRightBigTick(true);
                    waitForSecs(0.3);
                }
                nextScene();
            }
            else
            {
                setStatus(STATUS_AWAITING_CLICK);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}