package isac.gameoflife;

import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.Tap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class TestGridView {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);


    @Test
    public void testSwipe(){
        GridView gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);

        assertTrue(gridView.getInfoSwipe()==null);

        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                GeneralLocation.TOP_LEFT,GeneralLocation.BOTTOM_RIGHT, Press.FINGER));

        assertTrue(gridView.getInfoSwipe()==null);

        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT, Press.FINGER));

        assertTrue(gridView.getInfoSwipe().first.second==PinchInfo.Direction.RIGHT);

        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                GeneralLocation.CENTER,GeneralLocation.TOP_CENTER, Press.FINGER));

        assertTrue(gridView.getInfoSwipe().first.second==PinchInfo.Direction.UP);

        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                GeneralLocation.CENTER,GeneralLocation.CENTER_LEFT, Press.FINGER));

        assertTrue(gridView.getInfoSwipe().first.second==PinchInfo.Direction.LEFT);

        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                GeneralLocation.CENTER,GeneralLocation.BOTTOM_CENTER, Press.FINGER));

        assertTrue(gridView.getInfoSwipe().first.second==PinchInfo.Direction.DOWN);

    }

    @Test
    public void testSettingCell(){
        setCell();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setCell();
    }


    @Test
    public void testGame(){
        GridView gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);

        //sets the cells on screen
        setCell();

        //double tap to start the game
        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.DOUBLE, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{100,600};
            }
        }, Press.FINGER));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(gridView.isStarted());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.DOUBLE, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{100,600};
            }
        }, Press.FINGER));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(gridView.isStarted());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setCell(){
        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.LONG, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{280,600};
            }
        }, Press.FINGER));

        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.LONG, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{380,600};
            }
        }, Press.FINGER));

        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.LONG, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{450,600};
            }
        }, Press.FINGER));
    }
}
