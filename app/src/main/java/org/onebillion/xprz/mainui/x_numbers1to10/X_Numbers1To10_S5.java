package org.onebillion.xprz.mainui.x_numbers1to10;

import org.onebillion.xprz.controls.OBControl;
import org.onebillion.xprz.mainui.generic.XPRZ_Generic;

import java.util.EnumSet;

/**
 * Created by pedroloureiro on 12/07/16.
 */
public class X_Numbers1To10_S5 extends X_Numbers1To10_S4
{

    public void action_answerIsCorrect (OBControl target) throws Exception
    {
        gotItRightBigTick(true);
        waitForSecs(0.3);
        //
        playAudioQueuedScene(currentEvent(), "CORRECT", true);
        //
        action_lowlight(target);
        //
        playAudioQueuedScene(currentEvent(), "FINAL", true);
        //
        nextScene();
    }

    public void demo5a() throws Exception
    {
        setStatus(STATUS_DOING_DEMO);
        loadPointer(POINTER_MIDDLE);
        //
        action_playNextDemoSentence(false); // Let's count the flowers.
        XPRZ_Generic.pointer_moveToRelativePointOnScreen(0.5f, 0.5f, -15f, 0.6f, true, this);
        waitAudio();
        waitForSecs(0.3);
        //
        for (int i = 1; i <= 7; i++)
        {
            XPRZ_Generic.pointer_moveToObjectByName(String.format("obj_%d", i), (i < 4) ? -25+5*i : -20+5*i, (i == 1 || i == 4) ? 0.6f : 0.3f, EnumSet.of(XPRZ_Generic.Anchor.ANCHOR_MIDDLE), true, this);
            action_playNextDemoSentence(true); // One. Two. Three. Four. Five. Six. Seven
        }
        action_playNextDemoSentence(false); // Seven flowers altogether.
        XPRZ_Generic.pointer_moveToObjectByName("number_7", 10, 0.6f, EnumSet.of(XPRZ_Generic.Anchor.ANCHOR_MIDDLE), true, this);
        action_highlight(objectDict.get("number_7"));
        playSfxAudio("correct", true);
        XPRZ_Generic.pointer_moveToObjectByName("number_7", 10, 0.6f, EnumSet.of(XPRZ_Generic.Anchor.ANCHOR_BOTTOM), true, this);
        waitAudio();
        waitForSecs(0.3);
        //
        thePointer.hide();
        waitForSecs(0.7);
        //
        nextScene();
    }

}
