package com.lexis.speedometer;

import android.app.Service;
import android.content.Context;
import android.support.animation.FloatPropertyCompat;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class SpeedometerAdapter extends PagerAdapter {

    public static class Item implements IDataListener{
        private View root;
        private SpeedometerView speedometerView;
        private SpringAnimation arrowAnimator;
        private float defStiffness;

        Item(View root, float animStiffness) {
            this.root = root;
            this.defStiffness = animStiffness;
            this.speedometerView = this.root.findViewById(R.id.data_view);

            arrowAnimator  = new SpringAnimation(speedometerView, new FloatPropertyCompat<SpeedometerView>("value") {
                @Override
                public float getValue(SpeedometerView view) {
                    return view.getValue();
                }

                @Override
                public void setValue(SpeedometerView view, float value) {
                    view.setValue(value);
                }
            }, 0);
            arrowAnimator.setMinValue(0.0f);
            arrowAnimator.setMaxValue(speedometerView.getMaxValue());
            arrowAnimator.setMinimumVisibleChange(speedometerView.getMaxValue()/1000);
            arrowAnimator.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
            arrowAnimator.getSpring().setStiffness(animStiffness);
        }

        View getView() {
            return root;
        }

        SpeedometerView getSpeedometerView() {
            return speedometerView;
        }

        @Override
        public void onNewValue(double value) {
            float fval = (float)value;
            speedometerView.setRealValue(fval);
            arrowAnimator.getSpring().setFinalPosition(fval);

            float stiffness = defStiffness * 20 * (Math.abs(speedometerView.getValue() - fval)/speedometerView.getMaxValue()) + 10;

            arrowAnimator.getSpring().setStiffness(stiffness);
            arrowAnimator.start();
        }
    }

    private LayoutInflater inflater;
    private IDataProvider dataProvider;

    SpeedometerAdapter(Context context, IDataProvider dataProvider) {
        inflater = (LayoutInflater)context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        this.dataProvider = dataProvider;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((Item)object).getView();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int layout = (position == 0) ? R.layout.speedometer_page : R.layout.tachometer_page;
        View view = inflater.inflate(layout, container, false);
        container.addView(view, position);

        Item item = new Item(view, position == 0 ? SpringForce.STIFFNESS_MEDIUM : 5.0f);
        dataProvider.registerListener(item.getSpeedometerView().getChannel(), item);
        return item;
    }
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Item item = (Item) object;
        dataProvider.unregisterListener(item.getSpeedometerView().getChannel(), item);
        container.removeView(item.getView());
    }
}
