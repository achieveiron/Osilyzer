package com.thesky.rpark.osilyzer;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

public class PowerBalanceAdapter extends RecyclerView.Adapter<PowerBalanceAdapter.BalanceViewHolder> {
    private Context mContext;
    private List<PowerBalance> balanceList;

    public class BalanceViewHolder extends RecyclerView.ViewHolder{
        public TextView title;
        public ImageView overflow;
        public LineChart mChart;

        public BalanceViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            overflow = (ImageView) itemView.findViewById(R.id.overflow);
            mChart = (LineChart) itemView.findViewById(R.id.chart);

            mChart.setDrawGridBackground(false);
            mChart.getDescription().setEnabled(false);
            mChart.setTouchEnabled(true);
            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            mChart.setPinchZoom(false);
            mChart.getAxisLeft().setDrawGridLines(false);
            mChart.getAxisRight().setEnabled(false);
            mChart.getXAxis().setDrawGridLines(true);
            mChart.getXAxis().setDrawAxisLine(false);
            Legend l = mChart.getLegend();
            l.setEnabled(false);

            LineData data = new LineData();
            mChart.setData(data);

        }
    }

    public PowerBalanceAdapter(Context mContext, List<PowerBalance> balanceList) {
        this.mContext = mContext;
        this.balanceList = balanceList;
    }

    @Override
    public BalanceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.power_balance, parent, false);
        return new BalanceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final BalanceViewHolder holder, int position) {
        PowerBalance balance = balanceList.get(position);
        holder.title.setText(balance.getName());

        holder.overflow.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                showPopupMenu(holder.overflow);
            }
        });
        addEntry(holder.mChart, balance.getLineData());
    }

    private void showPopupMenu(View view){
        PopupMenu popup = new PopupMenu(mContext, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_album, popup.getMenu());
        popup.setOnMenuItemClickListener(new MyMenuItemClickListener());
        popup.show();
    }

    class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener{
        public MyMenuItemClickListener() {
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()){
                case R.id.action_add_favourite:
                    return true;
                case R.id.action_play_next:
                    return true;
                default:
            }
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return balanceList.size();
    }

    private void addEntry(LineChart chart, float value) {
        LineData data = chart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), value), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(120);
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setColor(Color.BLACK);
        set.setLineWidth(0.5f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        set.setDrawFilled(false);

        return set;
    }
}
